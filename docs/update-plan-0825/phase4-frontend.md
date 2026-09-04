# Phase 4 实施计划：前端实现（缴费趋势 + 操作审计）

> 所属计划：[statistics-module-extension-plan.md](./statistics-module-extension-plan.md)
> 规划日期：2026-08-25
> 阶段目标：在 `property-admin-web`（Vite + Vue 3 + TS + Element Plus + Pinia）落地两页统计界面——「缴费趋势」双轴折线图与「操作审计」筛选表格 + 汇总卡片 + 导出，并补齐 API 封装、路由与侧边栏菜单，完成前后端联调。

---

## 1. 范围

**本阶段做**（纯前端，后端接口 Phase 1/2/3 已完成）：
- 引入 **echarts** 依赖。
- 新增 `FeeTrendView.vue`（时间范围选择 + 双轴折线图）。
- 新增 `AuditLogList.vue`（筛选 + 分页表格 + 汇总卡片 + 导出按钮）。
- 新增 `src/api/statistic.ts`（四个接口封装 + 类型）。
- 注册路由 + 侧边栏「报表统计」分组菜单。

**前置依赖（后端小改，需先确认）**：
- 将 `/api/admin/statistic/audit/log` 由 `GET + @RequestBody` 改为 `GET + @ModelAttribute`（与 `/audit/summary`、`/audit/export` 一致），使前端能用 URL 查询参数筛选。**若维持现状**，前端须用 `axios GET + data` 的非标准方式，风险较高，不建议。

**不含**：Dashboard / Report 迁移、后端改动（除上述必备的 `/audit/log` 归一化外）。

---

## 2. 现状盘点（已确认）

| 位置 | 现状 |
|------|------|
| `package.json` | **无 echarts**，需首批安装 |
| `src/api/*.ts` | 用 `@/utils/request`（axios 实例）；JSON 响应被拦截器拆成 `res`（含 `code/msg/data`），`blob` 响应直接返回 `response.data` |
| `src/router/index.ts` | 扁平 `children` 注册 + 懒加载 + `meta.title`，左栏面包屑读 `route.meta.title` |
| `src/layouts/MainLayout.vue` | 侧边栏 `el-menu router`；收费管理分组 `index="2"`，车位管理 `index="3"` |
| `src/components/CountUp.vue` | 已有，`<CountUp :end-val="n" :prefix suffix />` 滚动数字 |
| 导出范例 | `ReportView.vue` 用 `responseType:'blob'` + `URL.createObjectURL` 下载 |
| 后端 endpoint | `GET /api/admin/statistic/fee/trend`、`/audit/log`、`/audit/summary`、`/audit/export` |

---

## 3. 后端已提供接口与类型契约

| 接口 | 入参（Query） | 返回 |
|------|--------------|------|
| `GET /api/admin/statistic/fee/trend` | `start`/`end`（`yyyy-MM-dd`，可空，默认近 30 天） | `{ code,msg,data: FeeTrendPointVO[] }` |
| `GET /api/admin/statistic/audit/log` | `pageNum`/`pageSize`/`start`/`end`/`status`/`module`/`userName`/`keyword` | `{ code,msg,data: Page<AuditLogVO> }` |
| `GET /api/admin/statistic/audit/summary` | `start`/`end`/`status`（可空） | `{ code,msg,data: AuditSummaryVO }` |
| `GET /api/admin/statistic/audit/export` | 同 `audit/log` 筛选 | 直接 `xlsx` 文件流（`responseType:'blob'`） |

### 类型字段（与后端 VO 对齐）

```ts
// FeeTrendPointVO
interface FeeTrendPoint { date: string; payerCount: number; amount: number }

// AuditLogVO（id/traceId 建议走 string，规避雪花ID精度损失）
interface AuditLogItem {
  id: string
  traceId: string
  userName: string
  realName: string
  module: string
  action: string
  requestMethod: string
  requestUrl: string
  ipAddress: string
  status: 0 | 1            // 1成功 0失败
  resultCode: number
  resultMsg: string
  costTime: number
  createTime: string       // yyyy-MM-dd HH:mm:ss
}

// AuditSummaryVO
interface AuditModuleStat { module: string; count: number }
interface AuditUserStat { userName: string; realName: string; count: number }
interface AuditSummary {
  totalCount: number
  failCount: number
  failRate: number          // 百分比
  moduleTop: AuditModuleStat[]
  userTop: AuditUserStat[]
  riskActionCount: Record<string, number>
}

// 分页返回（MyBatis-Plus Page 序列化）
interface Page<T> { records: T[]; total: number; current: number; size: number }
```

---

## 4. 任务拆解

### 4.1 引入 ECharts（先做，验证可构建）
- `npm install echarts`
- 按需引入（控制体积）：`echarts/core` + `LineChart` + `Grid/Tooltip/Legend` + `CanvasRenderer`。
- 封装轻量 `useEChart`（或页面内 `init/setOption/resize/dispose`），管理容器生命周期与 `ResizeSensor`/`window.resize`。

### 4.2 新增 API 封装 `src/api/statistic.ts`
```ts
export function getFeeTrend(start?: string, end?: string) {
  return request.get('/api/admin/statistic/fee/trend', { params: { start, end } })
}
export function getAuditLogs(params: AuditLogQuery) {
  return request.get('/api/admin/statistic/audit/log', { params })
}
export function getAuditSummary(params: AuditSummaryQuery) {
  return request.get('/api/admin/statistic/audit/summary', { params })
}
export function exportAuditLogs(params: AuditLogQuery) {
  return request.get('/api/admin/statistic/audit/export', { params, responseType: 'blob' })
}
```
- 补充 `AuditLogQuery` 类型（`pageNum/pageSize/start/end/status/module/userName/keyword`，均可空，`start/end` 传 `YYYY-MM-DDTHH:mm:ss`）。

### 4.3 缴费趋势页 `src/views/report/FeeTrendView.vue`
- 控件：`el-date-picker type="daterange"`（或快捷近 7/30/90 天）默认近 30 天。
- 图表：**双轴折线** `LineChart`——左 Y 轴 `payerCount`（人数）、右 Y 轴 `amount`（金额，单位元）。
- 空态/加载态：无数据（数组全 0）显示 `el-empty`；接口异常 `ElMessage.error`。
- 后端已对连续日期补零，折线连续；金额 axis formatter 保留千分位与 `¥`。

### 4.4 操作审计页 `src/views/audit/AuditLogList.vue`
- 筛选栏（顶部 `el-form inline`）：操作者 `userName`、模块 `module`、结果 `status`(select 成功/失败)、时间范围 `daterange`、`查询/重置`。
- **汇总卡片**（`CountUp` + 读 `/audit/summary` 与 `/audit/log` 联动同参数）：
  - 总操作数 `totalCount`、失败数 `failCount`、失败率 `failRate%`、风险动作次数 `Σ riskActionCount`。
  - 说明：`totalCount` 为**当前查询区间**口径（非"今日"），卡片标题标注区间；如需"今日"需另加接口，本次不做。
- **表格**：时间、操作者、模块、动作、请求方法/URL、IP、状态(`el-tag` 成功/失败)、耗时、结果；分页组件 `el-pagination`（读 `Page.total/current/size`）。
- **风险动作**：`moduleTop`/`userTop` 仅作辅助展示（可选小表），风险动作次数在汇总卡片体现，避免页面过度堆叠。

### 4.5 导出按钮（复用 ReportView 下载模式）
```ts
const res = await exportAuditLogs(params)
const blob = new Blob([res as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
const url = URL.createObjectURL(blob)
const a = document.createElement('a'); a.href = url; a.download = `操作审计_${now}.xlsx`; a.click()
URL.revokeObjectURL(url)
```
- 导出使用**当前筛选参数**（忽略 `pageNum/pageSize`，导全量）。

### 4.6 路由 + 侧边栏菜单
- `router/index.ts` 注册（懒加载）：
  ```ts
  { path: 'report/trend', name: 'FeeTrend', component: () => import('@/views/report/FeeTrendView.vue'), meta: { title: '缴费趋势' } },
  { path: 'audit', name: 'AuditLog', component: () => import('@/views/audit/AuditLogList.vue'), meta: { title: '操作审计' } },
  ```
- `MainLayout.vue` 在「收费管理」分组后新增「报表统计」分组 `el-sub-menu index="4"`：
  ```html
  <el-sub-menu index="4">
    <template #title><el-icon><TrendCharts /></el-icon><span>报表统计</span></template>
    <el-menu-item index="/report/trend">缴费趋势</el-menu-item>
    <el-menu-item index="/audit">操作审计</el-menu-item>
  </el-sub-menu>
  ```
  - 图标 `TrendCharts`、`List` 从 `@element-plus/icons-vue` 引入。

---

## 5. 验收标准

1. `npm run dev` 正常；`dashboard`、既有页面不受影响。
2. 缴费趋势：选择区间加载双轴折线，人数/金额分轴展示，无数据时友好空态。
3. 操作审计：按筛选条件正确分页；汇总卡片数字随区间联动；失败率无除零异常（后端已保护）。
4. 导出：浏览器下载合法 `xlsx`，列与筛选数据一致。
5. 菜单/路由：侧边栏「报表统计」分组可见、跳转正确、面包屑标题正确。
6. 类型约束：无 `any` 泄漏，`vue-tsc` 通过（`npm run build`）。

---

## 6. 风险与规避

| 风险 | 规避 |
|------|------|
| echarts 安装失败 | 第一阶段先 `npm install echarts` 并最小化按需注册，确认构建通过 |
| `/audit/log` GET+body 兼容性 | 前置归一化为 `@ModelAttribute`；若不能改后端则用 axios GET `data` 透传并回归验证 |
| 雪花ID前端精度丢失 | `AuditLogItem.id/traceId` 走 `string` |
| 汇总"今日"口径误解 | 卡片明确标注"区间"，不做今日单独接口 |
| 过渡堆叠/样式不一致 | 复用 Element Plus 卡片/表格/CountUp 现有风格 |

---

## 7. 工作量与遗留

- 预估：`npm install echarts` + 1 个 API 文件 + 2 个视图 + 路由 + 菜单 + 联调自测。
- 待确认：`/audit/log` 是否允许后端归一化为 `@ModelAttribute`（前端方案强依赖此）。
- 遗留：缴费趋势按模块/收费项下钻、地图/环形占比、审计趋势（操作次数随时间的折线）等可视化为可选增强，可排后续迭代。