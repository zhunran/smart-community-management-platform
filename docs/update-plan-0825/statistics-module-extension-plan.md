# property-module-statistic 扩展实施计划

> 规划日期：2026-08-25
> 目标：在不进行跨模块破坏性迁移的前提下，将「报表统计」能力补充进 `property-module-statistic` 空模块，覆盖「财务缴费趋势」与「操作审计」两大类统计，并同步补齐管理端前端可视化。

---

## 一、背景与目标

### 现状
- `property-module-statistic` 模块目前**只有 `pom.xml`**，无任何实现。
- 既有「收缴率 / 账期聚合」在 `property-module-bill` 的 `DashboardService`，本质是**时点状态快照**。
- 「收费报表导出」在 `admin-api` 的 `ReportExportService`。
- 「操作审计日志」数据已由 `OperationLogAspect` 埋点落库到 `t_sys_operation_log`，但**没有任何查看/统计界面**。

### 需求
1. **财务缴费趋势（时间序列）**：每日缴费人数 + 缴费金额的趋势变化，用前端图表（折线图）可视化呈现——区别于现有仪表盘的"状态快照"，提供"过程趋势"视角。
2. **操作审计**：把已埋点的 `t_sys_operation_log` 变成可查询、可统计、可导出的审计能力。
3. **不迁移**：财务统计接口与 Dashboard/Report 保持原位，`statistic` 模块**新增**子功能，降低回归风险。

---

## 二、模块与依赖设计

### 依赖方向（单向、无循环）

```
property-common  ←── property-framework  ←── property-module-bill
                                                  ↑
                                          property-module-statistic（新增业务层/展示聚合）
                                                  ↑
                                          property-admin-api（Controller 聚合，面向前端）
```

- `property-module-statistic` 增加依赖：
  - `property-framework`（读 `SysOperationLogMapper`，做审计统计）
  - `property-module-bill`（读 `PaymentOrderMapper`，做缴费趋势；`BillPaymentMapper` 次选）
- `property-admin-api` 增加 `property-module-statistic` 依赖，把统计服务暴露为 REST。
- `@ConditionalOnBean` 注意：`PaymentOrderService` 仅在 `AlipayService` Bean 存在时装配。**统计服务不依赖 payment 模块的 Service Bean**，直接读 `PaymentOrderMapper`，避免在未装支付的部署中失效。

### 目录规划（新建于 `property-module-statistic`）

```
property-module-statistic/src/main/java/com/property/module/statistic/
├── dto/response/
│   ├── FeeTrendPointVO.java        # 每日缴费趋势点（date, payerCount, amount）
│   └── AuditLogVO.java             # 审计日志查询结果
├── repository/
│   └── statistic sql 查询（复用 framework/bill 模块 Mapper + 新增聚合方法）
├── service/
│   ├── FeeTrendStatService.java    # 财务缴费趋势
│   └── AuditLogStatService.java    # 操作审计查询/统计
└── /（Controller 由 admin-api 提供，statistic 只做业务层）
```

> statistic 模块保持「业务层」（service + dto + sql），Controller 统一放到聚合层 `property-admin-api`，与现有架构一致。

---

## 三、后端实现

### 1. 财务缴费趋势（每日人数 + 金额）

**数据源**：`t_payment`（`PaymentOrderMapper`，位于 `property-module-bill`）

**核心口径**（重要）：
- 仅统计**支付成功**记录（在线支付回调置 2、线下支付创建即 2），过滤 `payment_status = 2`（失败/待支付不入趋势）。
- 「缴费人数」用 `COUNT(DISTINCT owner_id)` 去重；「缴费金额」用 `SUM(payment_amount)`。
- 时间按 `payment_time` 所属自然日 `DATE(payment_time)` 分组。

**聚合 SQL（Mapper 新增方法）**：
```xml
<select id="selectDailyFeeTrend" resultType="...FeeTrendPointVO">
    SELECT DATE(payment_time)      AS date,
           COUNT(DISTINCT owner_id) AS payer_count,
           SUM(payment_amount)      AS amount
    FROM t_payment
    WHERE payment_status = 2
      AND del_flag = 0
      AND payment_time &gt;= #{start}
      AND payment_time &lt; #{end}
    GROUP BY DATE(payment_time)
    ORDER BY DATE(payment_time)
</select>
```
- 入参：`start` / `end`（起止时间，默认近 30 天）。
- 前端拿到 `[{date, payerCount, amount}]` 后用 ECharts 绘制**双轴折线图**（左轴缴费人数、右轴缴费金额）。
- **补零**：为让折线连续，可对无缴费的自然日补 `payerCount=0, amount=0`（简单做法在后端对连续日期做填充）。

**接口**：
```
GET /api/admin/statistic/fee/trend?start=2026-08-01&end=2026-08-25
→ { code, msg, data: [ { date: "2026-08-01", payerCount: 12, amount: 5600.00 }, ... ] }
```

### 2. 操作审计查询 + 统计

**数据源**：`t_sys_operation_log`（`SysOperationLogMapper`，位于 `property-framework`）

**能力**：
- **分页查询**：按操作者(`userName`)、模块(`module`)、动作(`action`)、结果(`status`)、时间范围筛选。
- **维度统计**：按模块/操作者聚合计数；**失败率**；**风险动作次数**（删除/缴费/导入/对账等，可从 `action` 前缀或动作清单匹配）。

**接口**：
```
GET /api/admin/statistic/audit/log     → 分页日志（含筛选条件）
GET /api/admin/statistic/audit/summary → 聚合统计（按模块/人 TopN、失败率、风险动作次数）
GET /api/admin/statistic/audit/export  → 审计日志导出 Excel（复用 EasyExcel，参考 ReportExportService）
```

### 3. 新增 Mapper 聚合方法落点
- `SysOperationLogMapper`（framework）：新增统计查询 `selectAuditSummary`。
- 缴费趋势建议直接加在 `PaymentOrderMapper`（bill 模块）或 statistic 模块内新建只读 SQL 片段。优先**在 statistic 模块内新增聚合 Mapper**，避免改动既有模块、保持 `statistic` 自包含，降低回归风险。

---

## 四、前端实现（property-admin-web）

### 1. 新增页面：缴费趋势
- 位置：`src/views/report/FeeTrendView.vue`（收敛到既有 `report` 目录，与收费报表一致）。
- 引入 **ECharts**（新依赖，业界标配）：
  ```
  npm install echarts
  ```
  按需按模块引入，控制体积：
  ```typescript
  import * as echarts from 'echarts/core'
  import { LineChart } from 'echarts/charts'
  import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
  import { CanvasRenderer } from 'echarts/renderers'
  ```
- 页面元素：
  - 时间范围选择器（默认近 30 天，可选近 7/30/90 天或自定义）。
  - **双轴折线图**：`payerCount`（左 Y 轴）+ `amount`（右 Y 轴，单位元）。
  - 加载态 / 空态 / 接口异常兜底。

### 2. 新增页面：操作审计
- 位置：`src/views/audit/AuditLogList.vue`。
- 表格列：时间、操作者、模块、动作、请求方法/URL、IP、状态、耗时、结果。
- 顶部筛选：操作者 / 模块 / 结果 / 时间范围。
- **汇总卡片**：今日操作数、总操作数、失败数、失败率。
- 导出按钮 → 调用审计导出接口。

### 3. 新增 API 封装
- `src/api/statistic.ts`：封装财务趋势、审计日志、审计汇总、审计导出四个接口，类型与后端 DTO 对齐（`id` 类字段走 `string`，沿用雪花ID已修订规范）。

### 4. 路由与菜单
- 在 `MainLayout` 侧边栏添加分组「报表统计」：
  - 缴费趋势（`/report/trend`）
  - 操作审计（`/audit`）
- 在 `router/index.ts` 注册对应路由与懒加载。

### 5. 图标与交互一致性
- 沿用现有 Element Plus 组件风格与 `CountUp` 组件展示汇总数字。

---

## 五、接口一览（汇总）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/statistic/fee/trend` | 每日缴费人数/金额趋势 |
| GET | `/api/admin/statistic/audit/log` | 操作审计日志分页 |
| GET | `/api/admin/statistic/audit/summary` | 审计聚合统计 |
| GET | `/api/admin/statistic/audit/export` | 审计日志导出 Excel |

---

## 六、数据口径与边界

- **缴费状态**：仅 `payment_status = 2`（支付成功）计入趋势，与支付模块枚举 `PaymentStatusEnum` 对齐。
- **人数去重**：`COUNT(DISTINCT owner_id)`，防止同人多笔缴费把趋势虚高。
- **审计日志**：`status=1` 成功、`status=0` 失败（沿用 `SysOperationLogEntity` 约定）。
- **模块边界**：本次**不做 Dashboard / Report 迁移**，`statistic` 仅新增统计子功能；中长期可视需要再把聚合报表下沉统一承载。

---

## 七、任务拆解与里程碑

> 按依赖关系与交付顺序划分为四个阶段（工作量拆分见各阶段文档）。依赖准备（`property-module-statistic` pom 依赖、`property-admin-api` 依赖 statistics、parking/notification 去冗余）已先行完成。

### Phase 1：财务缴费趋势
- [ ] 新建 `FeeTrendPointVO`（date / payerCount / amount）。
- [ ] 在 statistic 模块内新增缴费趋势聚合查询（`payment_status=2`、`COUNT(DISTINCT owner_id)`、按自然日分组、连续日期补零）。
- [ ] 新建 `FeeTrendStatService`。
- [ ] `AdminStatisticController` 暴露 `GET /api/admin/statistic/fee/trend`。
- [ ] 编译 + 接口自测（curl）。
- 详见：[phase1-fee-trend.md](./phase1-fee-trend.md)

### Phase 2：操作审计
- [ ] 复用 `SysOperationLogEntity`，新建审计日志分页查询（按操作者/模块/动作/结果/时间筛选）。
- [ ] 新建 `AuditLogStatService`。
- [ ] `AdminStatisticController` 暴露 `GET /api/admin/statistic/audit/log`。
- [ ] 编译 + 接口自测。

### Phase 3：聚合方法添加
- [ ] 新增审计聚合统计（按模块/操作者 TopN、失败率、风险动作次数）→ `GET /api/admin/statistic/audit/summary`。
- [ ] 新增审计日志导出（复用 EasyExcel，参考 `ReportExportService`）→ `GET /api/admin/statistic/audit/export`。
- [ ] 操作日志 Mapper 统计方法落位；编译 + 接口自测。

### Phase 4：前端实现
- [ ] 引入 ECharts。
- [ ] 新建 `FeeTrendView.vue`（双轴折线图 + 时间范围选择）。
- [ ] 新建 `AuditLogList.vue`（筛选表格 + 汇总卡片 + 导出按钮）。
- [ ] 新增 `statistic.ts` API + 路由 + 侧边栏菜单（报表统计分组）。
- [ ] 前端联调 + 样式走查 + 验证与收尾。

---

## 八、风险与规避

| 风险 | 规避 |
|------|------|
| ECharts 未安装导致构建失败 | Phase 3 第一步先 `npm install echarts`，确认后再写页面 |
| 依赖方向引入循环 | 严格单向：statistic → framework/bill，admin-api → statistic |
| 与既有 Dashboard 语义重复 | 明确区分：Dashboard=状态快照，statistic=过程趋势+审计 |
| 缴费数据不足导致曲线空白 | 后端对连续日期补零，空态友好提示 |