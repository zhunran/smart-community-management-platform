# Phase 2 实施计划：操作审计

> 所属计划：[statistics-module-extension-plan.md](./statistics-module-extension-plan.md)
> 规划日期：2026-08-25
> 阶段目标：把已由 `OperationLogAspect` 埋点落库的 `t_sys_operation_log` 变为可查询的操作审计列表，提供按 操作者/模块/动作/结果/时间 的筛选与分页。**本阶段只做查询**（审计统计汇总与导出放到 Phase 3）。

---

## 1. 范围

**本阶段产出**：
- `AuditLogStatService`：审计日志分页查询
- `AuditLogVO`：查询结果 VO（剔除大字段，见 §3.1）
- `GET /api/admin/statistic/audit/log` 接口（复用餐费趋势 Controller `AdminStaticController` 或新增，见 §3.4）

**不含**：审计聚合统计（TopN / 失败率 / 风险动作）、Excel 导出 —— 全部在 **Phase 3（聚合方法添加）** 处理。

---

## 2. 数据口径

| 项 | 口径 |
|----|------|
| 数据表 | `t_sys_operation_log`（`SysOperationLogEntity`，位于 `property-framework`） |
| Mapper | `SysOperationLogMapper extends BaseMapper<SysOperationLogEntity>`（已 `@Mapper`、已被 `@MapperScan` 覆盖，**无需新增 Mapper**） |
| 状态 | `status = 1` 成功、`status = 0` 失败 |
| 分页 | MyBatis-Plus `Page` + `PaginationInnerInterceptor`（framework 已注册） |
| 默认时间 | 近 30 天（含今天），可自定义 `start` / `end`（按 `create_time` 过滤） |
| 排序 | `create_time DESC`（最新在前） |

**边界**：`create_time` 走半开区间 `[start, end+1天)`，与 Phase 1 一致含头含尾。

---

## 3. 实现设计

### 3.1 VO：`AuditLogVO`
位于 `property-module-statistic/.../vo/AuditLogVO.java`

只暴露列表所需字段，**排除大字段** `requestParams` / `responseData`（避免列表体量过大），明细可后续按 `id` 单独查：

```java
@Data
public class AuditLogVO {
    private Long id;
    private String traceId;
    private String userName;    // 操作者账号
    private String realName;    // 操作者姓名
    private String module;      // 模块
    private String action;      // 动作
    private String requestMethod;
    private String requestUrl;
    private String ipAddress;
    private Integer status;     // 1成功 0失败
    private Integer resultCode;
    private String resultMsg;
    private Long costTime;
    private LocalDateTime createTime;
}
```

### 3.2 分页查询（复用 BaseMapper，无需新 Mapper）
底层由 MyBatis-Plus 生成，等价的原生 MySQL 如下（.NET 换行、`<`/`>` 转义示意）：

```sql
SELECT id, trace_id, user_name, real_name, module, action,
       request_method, request_url, ip_address,
       status, result_code, result_msg, cost_time, create_time
FROM t_sys_operation_log
WHERE create_time &gt;= #{start}                 -- 含 start 当天 00:00:00
  AND create_time &lt; DATE_ADD(#{end}, INTERVAL 1 DAY)  -- 含 end 当天
  [AND status = #{status}]                    -- 仅当 status 非空
  [AND module  LIKE CONCAT('%', #{module}, '%')]     -- 仅当 module 非空
  [AND real_name LIKE CONCAT('%', #{keyword}, '%')]  -- 仅当 keyword 非空（或不按姓名，改 user_id）
ORDER BY create_time DESC
LIMIT #{offset}, #{size}                      -- offset = (pageNum-1)*pageSize
```

> MyBatis-Plus 中 `LambdaQueryWrapper` 会生成等价 SQL，动态条件用 `.eq(boolean, ...)` / `.like(boolean, ...)` 判断可空，避免手拼 `WHERE`：

### 3.3 服务：`AuditLogStatService`
位于 `property-module-statistic/.../service/AuditLogStatService.java`

- 依赖 `SysOperationLogMapper`（来自 framework）。
- 方法：`Page<AuditLogVO> pageAuditLog(query)`，返回 VO 分页。
- 默认时间兜底逻辑与 Phase 1 复用同一套（都空→近30天；传单边→补30天）。
- DTO→VO 用 `BeanUtils.copyProperties`，逐条转换。

### 3.4 接口
```
GET /api/admin/statistic/audit/log?pageNum=1&pageSize=20
    &status=1&module=缴费管理&keyword=张三
    &start=2026-08-01&end=2026-08-25
→ { code:0, msg:"ok", data:{ records:[AuditLogVO...], total:.., pageNum, pageSize } }
```

- **放置位置**：与缴费趋势同属统计统计模块，建议统一放在已有 `AdminStaticController`（现有路径 `/api/admin/statistic/fee`），或新增专用 `AdminAuditController` 映射 `/api/admin/statistic/audit`。推荐**新增 `AdminAuditController`**，职责更清晰（缴费=财务趋势，审计=安全审计）。

---

## 4. 交付物清单

| 文件 | 归属模块 | 类型 |
|------|---------|------|
| `AuditLogVO.java` | statistic | 新增 |
| `AuditLogStatService.java` | statistic | 新增 |
| `AdminAuditController.java`（推荐） | admin-api | 新增 |

依赖前置（已完成）：`property-module-statistic` 依赖 `framework`（含 `SysOperationLogMapper`）；`property-admin-api` 依赖 statistics。

---

## 5. 验证

1. 触发若干带 `@OperationLog` 的写操作（增/改/删 + 缴费），确认 `t_sys_operation_log` 有数据。
2. `curl "http://localhost:8081/api/admin/statistic/audit/log?pageNum=1&pageSize=10&module=缴费管理"`：
   - 仅返回缴费模块日志，`create_time` 降序；
   - 翻页正常、`total` 正确；
   - 成功/失败分别命中 `status=1/0` 过滤。
3. 边界：只传 `start` 自动补 `end=start+29`；不传时间默认近30天；`start > end` 返回参数错误。

---

## 6. 工作量与遗留

- 预估：VO + 服务 + Controller + 接口自测。
- **不引入新的聚合 SQL**（复用 BaseMapper），因此无需新增 Mapper。
- 遗留：审计汇总统计（失败率/TopN/风险动作）、Excel 导出、`requestParams/responseData` 明细查询 → **Phase 3**；前端审计表格页 → **Phase 4**。