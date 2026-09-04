# Phase 1 实施计划：财务缴费趋势

> 所属计划：[statistics-module-extension-plan.md](./statistics-module-extension-plan.md)
> 规划日期：2026-08-25
> 阶段目标：完成「每日缴费人数 + 缴费金额」趋势后端的完整落地（DTO + 聚合查询 + 服务 + 接口），并经接口自测。前端折线图在 Phase 4 再做。

---

## 1. 范围

**本阶段只做后端**，产出：
- `FeeTrendPointVO`：趋势点 DTO
- 聚合查询：`t_payment` 按自然日分组统计每日缴费人数 / 缴费金额
- `FeeTrendStatService`：业务服务
- `AdminStatisticController`：暴露 `GET /api/admin/statistic/fee/trend`

**不含**：前端 ECharts（Phase 4）、操作审计（Phase 2 / 3）、Dashboard 迁移（明确不做）。

---

## 2. 数据口径

| 项 | 口径 |
|----|------|
| 数据表 | `t_payment`（`PaymentOrderEntity`，经 `bill` 模块的 `PaymentOrderMapper`） |
| 状态过滤 | `payment_status = 2`（`PaymentStatusEnum.SUCCESS`，支付成功；含线下支付与支付宝回调成功） |
| 软删除 | `del_flag = 0`（`@TableLogic`） |
| 缴费人数 | `COUNT(DISTINCT owner_id)` 去重，避免同一业主多笔缴费把人数冲虚高 |
| 缴费金额 | `SUM(payment_amount)` |
| 时间维度 | `DATE(payment_time)` 自然日分组 |
| 默认区间 | 近 30 天（含今天），支持自定义 `start` / `end` |

**边界**：待支付(0)、支付中(1)、失败(3)、退款(4/5) 一律不入趋势。

---

## 3. 实现设计

### 3.1 VO：`FeeTrendPointVO`
位于 `property-module-statistic/.../dto/response/FeeTrendPointVO.java`

```java
public class FeeTrendPointVO {
    private String date;          // 日期 yyyy-MM-dd
    private Long payerCount;      // 当日缴费人数（去重业主）
    private java.math.BigDecimal amount; // 当日缴费金额
}
```

### 3.2 聚合查询
新增只读聚合 Mapper，置于 statistic 模块内，**保持模块自包含、不改动既有模块**：

```
com.property.module.statistic.repository.StatisticAggMapper (或 FeeTrendMapper)
```

聚合 SQL：
```sql
SELECT DATE(payment_time)        AS date,
       COUNT(DISTINCT owner_id)  AS payer_count,
       SUM(payment_amount)       AS amount
FROM t_payment
WHERE payment_status = 2
  AND del_flag = 0
  AND payment_time &gt;= #{start}
  AND payment_time &lt; #{end}
GROUP BY DATE(payment_time)
ORDER BY DATE(payment_time)
```
- 入参：`LocalDateTime start`、`LocalDateTime end`。
- 逐行映射到 `FeeTrendPointVO`，字段用下划线自动映射（沿用 MyBatis-Plus snake_case 约定）。

### 3.3 连续日期补零
为提高折线图连续性，若查询区间内某自然日无缴费记录，后端在前填充：
```java
// 遍历 [startDate, endDate] 每个自然日，无记录的补 payerCount=0, amount=0
```

### 3.4 服务：`FeeTrendStatService`
```
com.property.module.statistic.service.FeeTrendStatService
  List<FeeTrendPointVO> getDailyFeeTrend(LocalDate start, LocalDate end)
```
- 默认时间区间由调用方 / Controller 兜底（未传则近 30 天）。
- 复用 `PaymentStatusEnum.SUCCESS` 语义（常量 2，避免魔数）。

### 3.5 接口：`AdminStatisticController`
```
GET /api/admin/statistic/fee/trend?start=2026-08-01&end=2026-08-25
→ { code:0, msg:"ok", data:[ {date:"2026-08-01", payerCount:12, amount:5600.00}, ... ] }
```
- 位置：`property-admin-api/.../controller/AdminStatisticController.java`。
- 校验：`start` 不得晚于 `end`、区间跨度上限（如 ≤ 366 天）防御。

---

## 4. 交付物清单

| 文件 | 归属模块 | 类型 |
|------|---------|------|
| `FeeTrendPointVO.java` | statistic | 新增 |
| `StatisticAggMapper.java`（聚合查询） | statistic | 新增 |
| `FeeTrendStatService.java` | statistic | 新增 |
| `AdminStatisticController.java` | admin-api | 新增 |

依赖前置（已完成）：`property-module-statistic` pom 含 `framework`/`bill`；`property-admin-api` 含 `property-module-statistic`。

---

## 5. 验证

1. 造数：在测试库插入若干 `payment_status=2`、`payment_time` 分布在不同日期的 `t_payment` 记录（含同一 `owner_id` 多笔缴费）。
2. `curl "http://localhost:8081/api/admin/statistic/fee/trend?start=2026-07-01&end=2026-08-25"`：
   - 返回 `data` 数组按日期升序；
   - 无缴费日补零；
   - 同人多笔只计 `payerCount=1`；
   - `amount` 为该日成功缴费金额合计。
3. 边界：`start > end` 返回参数错误；超长区间被拦；待支付/失败记录不出现。

---

## 6. 工作量与遗留

- 预估：DTO + 聚合查询 + 服务 + 接口 + 自测。
- 遗留：前端双轴折线图、时间范围选择器，统一在 **Phase 4（前端实现）** 处理。