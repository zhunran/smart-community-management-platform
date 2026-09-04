# Phase 3 实施计划：审计聚合统计 + 审计日志导出

> 所属计划：[statistics-module-extension-plan.md](./statistics-module-extension-plan.md)
> 规划日期：2026-08-25
> 阶段目标：在 Phase 2（审计日志分页查询）基础上，补齐「审计聚合统计」（按模块/操作者 TopN、失败率、风险动作次数）与「审计日志 Excel 导出」，形成审计闭环。Phase 3 只做后端，前端展示在 Phase 4。

---

## 1. 范围

**本阶段产出**：
- `AuditSummaryVO`：审计聚合统计 VO
- `AuditLogMapper` 新增两个只读统计方法（`selectAuditSummary`、`selectAuditLogExport`）
- `AuditLogStatService` 扩展：`getAuditSummary(query)`、`exportAuditLog(query)`
- 接口：`GET /api/admin/statistic/audit/summary`、`GET /api/admin/statistic/audit/export`

**不含**：前端汇总卡片/导出按钮页（Phase 4）。

---

## 2. 数据口径（与 Phase 2 强一致）

| 项 | 口径 |
|----|------|
| 数据表 | `t_sys_operation_log`（`SysOperationLogEntity`，framework） |
| 时间过滤 | 半开区间 `[start, end+1天)`，默认近 30 天，与 Phase 1/2 复用同一套兜底与校验 |
| 状态 | `status=1` 成功、`status=0` 失败 |
| 筛选 | 与 Phase 2 相同的 `module` / `action` / `userName` / `status` / `keyword` |

**差异提醒**：查询 DTO 走 `LocalDateTime`（`OperationLogQuery.start/end`），与 Phase 2 一致；聚合/导出复用同一 DTO，不新增查询参数类型。

---

## 3. 实现设计

### 3.1 聚合统计：`AuditSummaryVO`

位于 `property-module-statistic/.../vo/AuditSummaryVO.java`

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditSummaryVO {
    private Long totalCount;          // 统计区间内总操作数
    private Long failCount;           // 失败数
    private BigDecimal failRate;      // 失败率（failCount/totalCount，四舍五入保留2位）
    private List<AuditModuleStatVO> moduleTop;   // 按模块 TopN
    private List<AuditUserStatVO>   userTop;     // 按操作者 TopN
    private Map<String, Long> riskActionCount;   // 风险动作次数（按 action 计数）
}
```

内嵌子结构 `AuditModuleStatVO`（module, count）、`AuditUserStatVO`（userName, realName, count），随 `AuditSummaryVO` 分文件或合并在同一 vo 包。

### 3.2 聚合查询

新增只读聚合 Mapper，置于 statistic 模块内（沿用 Phase 1 的 `FeeTrendMapper` 风格，`@Select` + 返回 VO 投影，**不继承 BaseMapper**）：

**① 按模块计数（TopN）**
```sql
SELECT module, COUNT(*) AS count
FROM t_sys_operation_log
WHERE create_time >= #{start} AND create_time < #{end}
  [AND status = #{status}]
  [AND user_name = #{userName}]
GROUP BY module
ORDER BY count DESC
LIMIT 10
```

**② 按操作者计数（TopN）**
```sql
SELECT user_name, real_name, COUNT(*) AS count
FROM t_sys_operation_log
WHERE create_time >= #{start} AND create_time < #{end}
GROUP BY user_name, real_name
ORDER BY count DESC
LIMIT 10
```

**③ 风险动作次数（一次性取全部 action 计数，服务层筛选风险动作）**
```sql
SELECT action, COUNT(*) AS count
FROM t_sys_operation_log
WHERE create_time >= #{start} AND create_time < #{end}
GROUP BY action
```

**④ 总/失计数**：`SELECT COUNT(*) total, SUM(status=0) fail FROM ...`，或用两个 `eq(status=...)` 简单聚合复用。

> 说明：①②③④ 的 `WHERE` 前缀条件需与外层筛选一致（module/userName/status 可空），与 Phase 2 的 `LambdaQueryWrapper` 动态拼接思路一致——但聚合用原生 `@Select`，需要把可空条件写进 SQL 或拆分为多个更细方法。**建议**：若筛选项过多导致 SQL 分支爆炸，可只对"时间 + status"做公共过滤（绝大多数审计只看时间段与成败），`module/userName` 交互式下钻放前端二次查询。请在实现时按此取舍并与设计达成一致（见 §5 待确认）。

### 3.3 风险动作定义

在 statistic 模块常量类配置风险动作清单（与已埋点的 `@OperationLog` action 文案对齐）：
- 删除类：`删除业主` / `删除楼栋` / `删除房屋` 等（action 以"删除"开头的视为风险）
- 财务/导入/对账类：`整单缴费` / `分项缴费` / `手动对账` / `批量导入缴费记录` / `执行双轨对账`

实现：对 ③ 的结果，按 `action` 前缀或精确清单过滤，映射到 `riskActionCount`。风险清单建议收敛为静态集合，便于后续增删。

### 3.4 导出：`exportAuditLog`

- 复用 `EasyExcel`（确认其已在 `property-admin-api` / `property-framework` 的依赖里，参考 `ReportExportService` 现有写法）。
- 复用 Phase 2 的分页查询结果（不限条数或分批），把 `AuditLogVO` 直接写出行：列 = 时间/操作者/模块/动作/请求方法/URL/IP/状态/耗时/结果。
- 文件写入到 `HttpServletResponse` 的 `OutputStream`，`Content-Disposition` 中文文件名用 URL 编码。

```java
public void exportAuditLog(OperationLogQuery query, HttpServletResponse response) {
    List<AuditLogVO> rows = pageAuditLog(query, 不设页大小或拉全量).getRecords();
    EasyExcel.write(response.getOutputStream(), AuditLogExportRow.class)
             .sheet("操作审计")
             .doWrite(rows);
}
```

> 注意：`AuditLogVO` 字段带 `@Data` 可直接作 Excel 行模型，或单独建 `AuditLogExportRow` 用 `@ExcelProperty` 标注列名，避免 VO 与导出模型耦合。推荐独立导出行模型。

### 3.5 接口

```
GET /api/admin/statistic/audit/summary?start=...&end=...&status=...
→ { code, msg, data: AuditSummaryVO }

GET /api/admin/statistic/audit/export?start=...&end=...&module=...
→ 直接输出 xlsx 文件流（Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）
```

- 放置位置：复用 `AdminStaticController`（已含 `/audit/log`），新增同 `@RequestMapping("/api/admin/statistic")` 下两个 GET。
- `export` 不返回 `ApiResult` 包装，直接写流。

---

## 4. 交付物清单

| 文件 | 归属模块 | 类型 |
|------|---------|------|
| `AuditSummaryVO.java`（含子结构） | statistic | 新增 |
| `AuditLogStatMapper.java`（①②③④ 聚合查询） | statistic | 新增 |
| `AuditLogStatService.java`（扩展 summary + export） | statistic | 修改 |
| `AuditExportRow.java`（可选，Excel 行模型） | statistic 或 admin-api | 新增 |
| `AdminStaticController.java`（新增 summary/export 端点） | admin-api | 修改 |

依赖前置（已完成）：statistic → framework/bill；admin-api → statistic。需确认 EasyExcel 依赖可用。

---

## 5. 验证

1. 造数后触发若干带 `@OperationLog` 的写操作（含删除、缴费、对账），确认 `t_sys_operation_log` 存在成功与失败记录。
2. `curl ".../audit/summary?start=...&end=..."`：
   - `totalCount` 正确、`failCount`/`failRate` 符合预期；
   - `moduleTop`/`userTop` 按 count 降序且 ≤10；
   - `riskActionCount` 仅含删除/缴费/导入/对账类动作。
3. `curl ".../audit/export?..."`：返回合法 xlsx，浏览器可下载，列名正确、数据与筛选一致。
4. 边界：不传时间默认近 30 天；`start>end` 返回参数错误；超 1 年拦截；无数据时 `totalCount=0`、`moduleTop` 空数组而非 null；`failRate` 除数不为 0（totalCount=0 时返回 0）。

---

## 6. 工作量与遗留

- 预估：3 个 VO + 1 个聚合 Mapper（4 条 SQL）+ Service 扩展 2 方法 + Controller 2 端点 + 自测。
- 待确认（§要求先与设计对齐）：聚合 SQL 的可空筛选分支取舍（建议只做 时间+status 公共过滤）；EasyExcel 依赖落点（framework 还是 admin-api）。
- 遗留：前端汇总卡片 + 导出按钮 + 审计趋势图 → **Phase 4**。