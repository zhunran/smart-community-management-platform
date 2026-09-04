# 前端优化（T43–T54）+ Excel 导入事务保护 操作指南

> 编制日期：2026-08-14
> 范围：property-admin-web、property-owner-web、property-admin-api、property-framework
> 状态：**待审阅，暂不实施**

---

## 总览

| 编号 | 任务 | 涉及端 | 复杂度 | 需改后端 |
|------|------|--------|--------|----------|
| T43 | Token 存储改为 httpOnly Cookie | admin-web + owner-web + 后端 | 高 | 是 |
| T44 | 401 改为路由跳转 | admin-web + owner-web | 低 | 否 |
| T45 | 路由守卫 Token 过期校验 | admin-web + owner-web | 中 | 否 |
| T46 | 添加 404 路由页面 | admin-web + owner-web | 低 | 否 |
| T47 | 身份证号列表脱敏 | admin-web | 低 | 否 |
| T48 | 金额加千分位格式化 | admin-web + owner-web | 中 | 否 |
| T49 | 状态映射统一抽取 | admin-web + owner-web | 中 | 否 |
| T50 | 银行支付弹窗不被拦截 | owner-web | 中 | 否 |
| T51 | Vite 代理地址环境变量化 | admin-web + owner-web | 低 | 否 |
| T52 | 全量下拉改为搜索下拉 | admin-web | 中 | 否 |
| T53 | 全局注册图标按需引入 | admin-web + owner-web | 中 | 否 |
| T54 | blob 响应增加业务错误校验 | admin-web + owner-web | 中 | 否 |
| EX1 | Excel 导入事务保护 | admin-api | 中 | 是（自身） |

---

## T43：Token 存储改为 httpOnly Cookie

### 现状问题
- Token 存储在 `localStorage`，XSS 攻击可直接窃取
- 前端手动从 localStorage 读取并设置 `Authorization` 请求头

### 改造方案

#### 后端改动（4 个文件）

**1. AuthInterceptor.java** — 增加从 Cookie 读取 Token 的能力

```
extractToken() 方法逻辑：
  1. 先尝试从 Authorization 头读取（保持向后兼容）
  2. 若头中无 Token，尝试从 Cookie 读取（Cookie 名：token）
```

新增常量：`private static final String COOKIE_NAME = "token";`

**2. AdminAuthController.java** — 登录成功后写入 httpOnly Cookie

```
login() 方法返回前：
  Cookie cookie = new Cookie("token", response.getToken());
  cookie.setHttpOnly(true);
  cookie.setPath("/");
  cookie.setMaxAge((int)(jwtExpiration / 1000));
  response.addCookie(cookie);
```

需注入 `HttpServletResponse` 和 `@Value("${jwt.expiration}")`。

**3. OwnerAuthController.java** — 同上，业主端登录也写 Cookie

**4. 新增登出接口**（两个 Controller 各加一个）

```
POST /api/admin/auth/logout
POST /api/owner/auth/logout
  - 清除 Cookie（设置 maxAge=0）
  - 返回成功
```

> LoginResponse 中的 token 字段保留（前端可选择性忽略），不破坏现有接口契约。

#### 前端改动

**admin-web（5 个文件）：**

| 文件 | 改动 |
|------|------|
| `src/utils/request.ts` | 1. axios 实例添加 `withCredentials: true`<br>2. 请求拦截器移除手动设置 Authorization 头（Cookie 自动携带）<br>3. 保留 Authorization 头逻辑作为 fallback（兼容过渡期） |
| `src/stores/user.ts` | 1. 不再将 token 写入 localStorage<br>2. 仅存储 username/realName 等非敏感信息<br>3. logout 调用后端登出接口 |
| `src/api/auth.ts` | 新增 `logoutApi()` 函数 |
| `src/layouts/MainLayout.vue` | logout 时调用 `userStore.logout()`（该方法内部调 API） |
| `src/views/login/LoginView.vue`` | 登录成功后不再需要从 response 取 token（store 自动处理） |

**owner-web（4 个文件）：**

| 文件 | 改动 |
|------|------|
| `src/utils/request.ts` | 同 admin-web：添加 `withCredentials: true`，移除手动 Authorization |
| `src/stores/owner.ts` | 不再写 token 到 localStorage；logout 调后端 |
| `src/api/auth.ts` | 新增 `logoutApi()` |
| `src/views/home/HomeView.vue` | logout 调用 store.logout()（内部调 API） |

#### 注意事项
- CORS 配置已启用 `allowCredentials(true)`，无需修改
- 开发环境 Vite 代理会自动透传 Set-Cookie 头
- 生产环境 Nginx 同域部署，Cookie 直接生效
- Cookie 不设 `secure`（开发环境 HTTP），生产环境可通过 Nginx 配置 HTTPS

---

## T44：401 改为路由跳转而非 window.location

### 现状问题
`request.ts` 中 401 处理使用 `window.location.href = '/login'`，导致：
- 整页刷新，丢失 Pinia 状态
- 用户体验差（白屏闪烁）

### 改造方案

**admin-web + owner-web 各改 1 个文件：**

`src/router/index.ts` — 导出 router 实例（已经是 `export default router`，可直接 import）

`src/utils/request.ts` — 替换 401 处理逻辑：
```typescript
// 之前
window.location.href = '/login'

// 之后
import router from '@/router'
// ...
case 401:
  ElMessage.error('登录已过期，请重新登录')
  // admin-web:
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  // owner-web:
  localStorage.removeItem('owner_id')
  localStorage.removeItem('owner_name')
  localStorage.removeItem('owner_phone')
  router.push('/login')
  break
```

---

## T45：路由守卫增加 Token 过期校验

### 现状问题
路由守卫仅检查 Token 是否存在（`!!token.value`），不校验是否已过期。过期 Token 仍可访问页面，直到 API 返回 401。

### 改造方案

T43 改造后 Token 在 httpOnly Cookie 中，前端无法读取。因此采用以下策略：

1. **登录时**：后端在 LoginResponse 中返回 `expiresAt`（时间戳），前端存入 localStorage
2. **路由守卫**：检查 `expiresAt` 是否已过期，过期则跳转登录

#### 后端改动（配合 T43）

`LoginResponse.java`（admin）和 `OwnerLoginResponse.java`（owner）各新增字段：
```java
private Long expiresAt; // Token 过期时间戳（毫秒）
```

登录 Service 中设置：`response.setExpiresAt(System.currentTimeMillis() + jwtExpiration)`

#### 前端改动

**admin-web：**

| 文件 | 改动 |
|------|------|
| `src/stores/user.ts` | login() 中将 `expiresAt` 存入 localStorage |
| `src/router/index.ts` | beforeEach 中增加过期校验：<br>`const exp = localStorage.getItem('expiresAt')`<br>`if (exp && Date.now() > Number(exp)) → 清除登录态并跳转 /login` |

**owner-web：** 同上模式

> 注意：这是**前端主动校验**，即使被绕过，后端 AuthInterceptor 仍会拦截无效 Token（纵深防御）。

---

## T46：添加 404 路由页面

### 改造方案

**admin-web + owner-web 各改 1 个文件 + 新建 1 个文件：**

1. 新建 `src/views/NotFoundView.vue`：
```vue
<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="抱歉，您访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>
```

2. `src/router/index.ts` 在 routes 数组末尾添加：
```typescript
{
  path: '/:pathMatch(.*)*',
  name: 'NotFound',
  component: () => import('@/views/NotFoundView.vue'),
  meta: { requiresAuth: false },
}
```

---

## T47：身份证号列表脱敏

### 现状问题
[OwnerList.vue](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/views/owner/OwnerList.vue) 第 51 行直接展示完整身份证号：`{{ row.idCardNo }}`

### 改造方案

**admin-web（2 个文件）：**

1. 新建 `src/utils/format.ts`（若 T48 也需建此文件则合并）：
```typescript
/** 身份证号脱敏：前3位 + **** + 后4位 */
export function maskIdCard(idCard: string): string {
  if (!idCard || idCard.length < 7) return idCard || ''
  return idCard.substring(0, 3) + '****' + idCard.substring(idCard.length - 4)
}
```

2. `OwnerList.vue` 第 51 行改为：
```vue
{{ idCardTypeMap[row.idCardType] ?? '未知' }} {{ maskIdCard(row.idCardNo) }}
```
并在 `<script>` 中 import `maskIdCard`。

> 注意：编辑弹窗中仍显示完整身份证号（编辑需要），仅列表页脱敏。

---

## T48：金额加千分位格式化

### 现状问题
所有金额直接显示 `¥{{ row.totalAmount }}`，大数字如 12345.67 缺少千分位分隔，可读性差。

### 改造方案

**admin-web + owner-web 各建 1 个工具文件：**

`src/utils/format.ts`：
```typescript
/** 金额格式化：千分位 + 2位小数，如 1,234.56 */
export function formatMoney(amount: number | string | null | undefined): string {
  if (amount == null || amount === '') return '0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 金额格式化（带 ¥ 前缀） */
export function formatYuan(amount: number | string | null | undefined): string {
  return '¥' + formatMoney(amount)
}
```

#### admin-web 需修改的文件及位置

| 文件 | 当前写法 | 改为 |
|------|---------|------|
| `BillList.vue` L18 | `¥{{ row.totalAmount }}` | `{{ formatYuan(row.totalAmount) }}` |
| `BillList.vue` L19 | `¥{{ row.paidAmount }}` | `{{ formatYuan(row.paidAmount) }}` |
| `BillList.vue` L54 | `¥{{ row.unitPrice }}` | `{{ formatYuan(row.unitPrice) }}` |
| `BillList.vue` L56 | `¥{{ row.amount }}` | `{{ formatYuan(row.amount) }}` |
| `BillList.vue` L57 | `¥{{ row.paidAmount }}` | `{{ formatYuan(row.paidAmount) }}` |
| `BillList.vue` L60 | `¥{{ detail.totalAmount }}` 等 | 全部改用 formatYuan |
| `PaymentList.vue` L18 | `¥{{ row.paymentAmount }}` | `{{ formatYuan(row.paymentAmount) }}` |
| `PaymentList.vue` L37 | `¥{{ detail.paymentAmount }}` | `{{ formatYuan(detail.paymentAmount) }}` |
| `DashboardView.vue` | 检查金额显示 | 按需格式化 |

#### owner-web 需修改的文件及位置

| 文件 | 当前写法 | 改为 |
|------|---------|------|
| `BillsView.vue` | `&yen;{{ bill.totalAmount }}` | `&yen;{{ formatMoney(bill.totalAmount) }}` |
| `BillsView.vue` | `&yen;{{ bill.paidAmount }}` | `&yen;{{ formatMoney(bill.paidAmount) }}` |
| `BillDetailView.vue` | 所有 `&yen;{{ ... }}` | 使用 formatMoney |
| `BillDetailView.vue` | dueAmount computed | toFixed(2) → formatMoney |
| `PaymentView.vue` | `&yen;{{ totalAmount.toFixed(2) }}` | `&yen;{{ formatMoney(totalAmount) }}` |
| `PaymentView.vue` | `(bill.totalAmount - bill.paidAmount + ...).toFixed(2)` | formatMoney |
| `RecordsView.vue` | `&yen;{{ r.paymentAmount }}` | `&yen;{{ formatMoney(r.paymentAmount) }}` |
| `RecordsView.vue` | ElMessageBox 中 `¥${r.paymentAmount}` | `¥${formatMoney(r.paymentAmount)}` |
| `PaymentSuccessView.vue` | 检查是否有金额 | 按需格式化 |

---

## T49：状态映射统一抽取

### 现状问题
- **owner-web**：`statusMap` 在 BillsView、BillDetailView、PaymentView 三个文件中重复定义
- **admin-web**：`BILL_STATUS_MAP` 在 `bill.ts` 中已定义，但**缺少状态 5（已逾期）**

### 改造方案

#### admin-web（1 个文件）

`src/api/bill.ts` L101-103，补全状态 5：
```typescript
export const BILL_STATUS_MAP: Record<number, string> = {
  0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期',
}
```

BillList.vue 已 import `BILL_STATUS_MAP`，无需额外改动。

#### owner-web（新建 1 文件 + 修改 3 文件）

1. 新建 `src/constants/index.ts`：
```typescript
/** 账单状态映射 */
export const BILL_STATUS_MAP: Record<number, string> = {
  0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期',
}

/** 账单状态 → Tag 类型 */
export function billStatusTag(status: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 2) return 'success'
  if (status === 5) return 'danger'
  if (status === 1) return 'warning'
  return 'info'
}

/** 支付状态 → Tag 类型 */
export function payStatusTag(status: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
}
```

2. 修改 `BillsView.vue`：删除本地 `statusMap`、`statusName`、`statusTag`，改为 import
3. 修改 `BillDetailView.vue`：同上
4. 修改 `PaymentView.vue`：同上

---

## T50：银行支付弹窗不被拦截

### 现状问题
[PaymentView.vue](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/payment/PaymentView.vue) 中，支付流程为：
1. 用户点击"确认支付"
2. `await ElMessageBox.confirm()` 等待用户确认
3. `await createPayOrder()` 异步请求后端获取支付表单
4. `form.submit()` 提交表单跳转支付宝

当前使用 `form.submit()` 在当前页面跳转，不涉及 window.open，**实际不会被拦截**。但存在用户体验问题：支付完成后浏览器后退会重复提交表单。

### 改造方案

改为"同步打开新窗口 + 异步写入表单"模式：

```typescript
async function submitPay() {
  // ...校验...
  await ElMessageBox.confirm(...)

  // 1. 在用户点击事件同步链中打开空白窗口（不会被拦截）
  const popup = window.open('', '_blank')
  if (!popup) {
    ElMessage.error('请允许浏览器弹出窗口')
    return
  }

  payLoading.value = true
  try {
    const res = await createPayOrder({ billId, paymentMethod: 1, payerName: '业主' })
    const data = res.data as any

    if (data.payFormHtml) {
      // 2. 将支付表单写入新窗口并提交
      popup.document.write(data.payFormHtml)
      popup.document.close()
      const form = popup.document.querySelector('form')
      if (form) form.submit()

      // 3. 关闭支付窗口或跳转到成功页（由 return_url 控制）
      selectedIds.value.clear()
      router.push('/records')
    } else {
      popup.close()
      router.push('/records')
    }
  } catch (e: any) {
    popup.close()
    ElMessage.error(e?.msg || e?.message || '支付失败')
  } finally {
    payLoading.value = false
  }
}
```

> 注意：`popup.document.write()` 在跨域场景下有限制。支付宝表单提交后会跳转到支付宝域名，初始写入是 about:blank，不受跨域限制。此方案是支付宝支付的标准前端实践。

---

## T51：Vite 代理地址环境变量化

### 现状问题
- admin-web `vite.config.ts` 硬编码 `http://localhost:8081`
- owner-web `vite.config.ts` 硬编码 `http://localhost:8084`

### 改造方案

**admin-web + owner-web 各改 1 个文件：**

`vite.config.ts`：
```typescript
export default defineConfig({
  // ...
  server: {
    port: 5173, // owner-web 为 5273
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8081', // owner-web 为 8084
        changeOrigin: true,
      },
    },
  },
})
```

可选：在项目根目录创建 `.env.development` 文件：
```
VITE_API_BASE_URL=http://localhost:8081
```

> .env 文件已在 .gitignore 中，不会提交。开发人员可按需创建。

---

## T52：全量下拉改为搜索下拉

### 现状问题
[OwnerList.vue](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/views/owner/OwnerList.vue) 第 320-329 行：
- `loadAllRooms()` 一次性加载 `size=9999` 条房屋数据
- 数据量大时前端卡顿、内存占用高

### 改造方案

**admin-web（仅改 OwnerList.vue）：**

将绑定房屋弹窗中的 `el-select` 改为远程搜索：

```vue
<el-select
  v-model="bindForm.roomId"
  placeholder="输入房号搜索"
  filterable
  remote
  :remote-method="searchRooms"
  :loading="roomSearchLoading"
  clearable
  style="width:100%"
>
  <el-option
    v-for="r in searchedRooms"
    :key="r.id"
    :label="`${r.buildingName} ${r.roomName}`"
    :value="r.id"
  />
</el-select>
```

Script 改动：
```typescript
const searchedRooms = ref<RoomVO[]>([])
const roomSearchLoading = ref(false)

async function searchRooms(query: string) {
  if (!query || query.length < 1) {
    searchedRooms.value = []
    return
  }
  roomSearchLoading.value = true
  try {
    const res = await pageRoom({ current: 1, size: 20, roomCode: query, status: 1 })
    searchedRooms.value = res.data.records || []
  } finally {
    roomSearchLoading.value = false
  }
}
```

删除 `onMounted` 中的 `loadAllRooms()` 调用和 `allRooms` 变量。

---

## T53：全局注册图标按需引入

### 现状问题
两个前端的 `main.ts` 都全量注册了 Element Plus 图标：
```typescript
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```
这会将所有图标打入 bundle，增加包体积。

### 改造方案

**admin-web + owner-web 各改 1 个文件：**

`src/main.ts` — 删除全局注册循环：
```typescript
// 删除以下代码：
// import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
//   app.component(key, component)
// }
```

#### 前置检查
经审查，所有视图文件使用的图标均已在本地 import：
- MainLayout.vue: `DataAnalysis, ArrowDown, HomeFilled, UserFilled, Money, Coin` ✓
- LoginView.vue (admin): `User, Lock` ✓
- LoginView.vue (owner): `Iphone, Lock, HomeFilled` ✓
- HomeView.vue (owner): `HomeFilled, ArrowDown, Document, Money, List, UserFilled` ✓
- PaymentSuccessView.vue: `ArrowLeft, CircleCheckFilled` ✓
- BillsView/BillDetailView/PaymentView: `ArrowLeft` ✓

> 实施时需全局搜索 `<el-icon>` 标签，确认所有用到的图标组件都有本地 import。若发现遗漏，补充 import 即可。

---

## T54：blob 响应增加业务错误校验

### 现状问题
导出 Excel 时，`request.ts` 对 blob 响应直接返回 `response.data`，不检查内容。若后端返回 JSON 错误（如 401 未登录、500 异常），前端会将 JSON 文本作为 Excel 文件下载，用户得到损坏的文件。

### 改造方案

**admin-web + owner-web 各改 1 个文件：**

`src/utils/request.ts` — 响应拦截器中增加 blob 内容检查：

```typescript
if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
  const contentType = response.headers['content-type'] || ''
  // 若返回的是 JSON（错误响应），解析并报错
  if (contentType.includes('application/json')) {
    return response.data.text().then((text: string) => {
      try {
        const errorData = JSON.parse(text)
        ElMessage.error(errorData.msg || '导出失败')
        return Promise.reject(new Error(errorData.msg || '导出失败'))
      } catch {
        ElMessage.error('导出失败')
        return Promise.reject(new Error('导出失败'))
      }
    })
  }
  return response.data
}
```

> 注意：`response.data` 在 blob 模式下是 Blob 对象，可调用 `.text()` 方法读取内容。

---

## EX1：Excel 导入事务保护

### 现状问题

[OwnerImportListener.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/excel/OwnerImportListener.java) 的 `batchInsert()` 和 [PaymentOrderImportListener.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/excel/PaymentOrderImportListener.java) 的 `batchProcess()` 中：
- 逐行调用 Service 方法，无事务包裹
- 单条失败仅 catch 记录，不影响其他行
- 若 Service 方法本身非事务性，可能导致部分写入（如业主创建了但关联数据未写入）

### 改造方案

使用 `TransactionTemplate` 编程式事务，**每行一个独立事务**（保证单条原子性），成功行提交、失败行回滚并记录错误。

> 不使用"整批一个事务"的原因：Excel 导入通常允许部分成功，整批回滚会导致 499 条成功因 1 条失败而全部撤销，用户体验差。

#### 改动文件（4 个）

**1. OwnerExcelService.java**

- 注入 `PlatformTransactionManager`
- 创建 `TransactionTemplate`（PROPAGATION_REQUIRES_NEW）
- 传给 `OwnerImportListener` 构造函数

```java
@Service
@RequiredArgsConstructor
public class OwnerExcelService {
    private final OwnerMapper ownerMapper;
    private final OwnerService ownerService;
    private final PlatformTransactionManager transactionManager;

    public OwnerImportListener importExcel(InputStream inputStream) throws IOException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        OwnerImportListener listener = new OwnerImportListener(ownerService, txTemplate);
        EasyExcel.read(inputStream, OwnerExcelVO.class, listener).sheet().doRead();
        return listener;
    }
}
```

**2. OwnerImportListener.java**

- 构造函数增加 `TransactionTemplate` 参数
- `batchInsert()` 中每行用 `txTemplate.executeWithoutResult()` 包裹

```java
private final TransactionTemplate txTemplate;

private void batchInsert() {
    for (OwnerExcelVO row : cachedList) {
        try {
            txTemplate.executeWithoutResult(status -> {
                OwnerCreateRequest request = buildCreateRequest(row);
                ownerService.create(request);
            });
            successCount++;
        } catch (Exception e) {
            log.warn("导入业主失败 [phone={}]: {}", row.getPhone(), e.getMessage());
            row.setValid(false);
            row.setErrorMsg(e.getMessage());
            failCount++;
            failList.add(row);
        }
    }
    cachedList.clear();
}
```

**3. PaymentOrderExcelService.java** — 暂不涉及导入，但为保持一致可检查

实际导入入口在 `AdminPaymentController.importPayments()` 中直接 new Listener，需改为通过 Service 方法或注入 TransactionTemplate。

**4. AdminPaymentController.java / 或新增 PaymentOrderExcelService.importExcel()**

将 Controller 中直接 `new PaymentOrderImportListener(billMapper, billService)` + EasyExcel.read 的逻辑移到 `PaymentOrderExcelService` 中，与 OwnerExcelService 保持一致的模式：

```java
// PaymentOrderExcelService 新增方法
public PaymentOrderImportListener importExcel(InputStream inputStream) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    PaymentOrderImportListener listener = new PaymentOrderImportListener(billMapper, billService, txTemplate);
    EasyExcel.read(inputStream, PaymentOrderImportVO.class, listener).sheet().doRead();
    return listener;
}
```

Controller 简化为：
```java
@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResult<ImportResult> importPayments(@RequestParam("file") MultipartFile file) throws IOException {
    PaymentOrderImportListener listener = paymentOrderExcelService.importExcel(file.getInputStream());
    // ...
}
```

**5. PaymentOrderImportListener.java** — 同 OwnerImportListener，增加 TransactionTemplate 参数，batchProcess 每行包裹事务

---

## 文件变更清单

### 新增文件（5 个）

| 文件 | 说明 |
|------|------|
| `property-admin-web/src/views/NotFoundView.vue` | 404 页面 |
| `property-admin-web/src/utils/format.ts` | 金额/身份证格式化工具 |
| `property-owner-web/src/views/NotFoundView.vue` | 404 页面 |
| `property-owner-web/src/utils/format.ts` | 金额格式化工具 |
| `property-owner-web/src/constants/index.ts` | 状态映射常量 |

### 修改文件

#### property-admin-web（11 个）

| 文件 | 任务 |
|------|------|
| `src/main.ts` | T53 删除图标全量注册 |
| `src/utils/request.ts` | T43/T44/T54 |
| `src/router/index.ts` | T44/T45/T46 |
| `src/stores/user.ts` | T43/T45 |
| `src/api/auth.ts` | T43 新增 logoutApi |
| `src/api/bill.ts` | T49 补全状态 5 |
| `src/layouts/MainLayout.vue` | T43 logout 调用 |
| `src/views/login/LoginView.vue` | T43 |
| `src/views/owner/OwnerList.vue` | T47/T52 |
| `src/views/bill/BillList.vue` | T48/T49 |
| `src/views/payment/PaymentList.vue` | T48 |
| `vite.config.ts` | T51 |

#### property-owner-web（12 个）

| 文件 | 任务 |
|------|------|
| `src/main.ts` | T53 |
| `src/utils/request.ts` | T43/T44/T54 |
| `src/router/index.ts` | T44/T45/T46 |
| `src/stores/owner.ts` | T43/T45 |
| `src/api/auth.ts` | T43 新增 logoutApi |
| `src/views/home/HomeView.vue` | T43 |
| `src/views/bills/BillsView.vue` | T48/T49 |
| `src/views/bills/BillDetailView.vue` | T48/T49 |
| `src/views/payment/PaymentView.vue` | T48/T49/T50 |
| `src/views/records/RecordsView.vue` | T48 |
| `src/views/payment/PaymentSuccessView.vue` | T48（检查） |
| `vite.config.ts` | T51 |

#### 后端（6 个）

| 文件 | 任务 |
|------|------|
| `AuthInterceptor.java` | T43 从 Cookie 读取 Token |
| `AdminAuthController.java` | T43 Set-Cookie + 登出接口 + expiresAt |
| `OwnerAuthController.java` | T43 同上 |
| `LoginResponse.java` | T45 新增 expiresAt 字段 |
| `OwnerLoginResponse.java` | T45 新增 expiresAt 字段 |
| `AdminPaymentController.java` | EX1 导入改为 Service 调用 |
| `OwnerExcelService.java` | EX1 注入 TransactionTemplate |
| `OwnerImportListener.java` | EX1 每行事务 |
| `PaymentOrderExcelService.java` | EX1 新增 importExcel 方法 + 事务 |
| `PaymentOrderImportListener.java` | EX1 每行事务 |

---

## 风险与注意事项

1. **T43 影响面最大**：涉及前后端认证机制变更，需完整回归测试登录/登出/Token 刷新流程。建议先在开发环境验证。
2. **T50 弹窗方案**：部分浏览器可能仍拦截 about:blank 弹窗，需测试 Chrome/Edge/Firefox。若有问题可降级为当前页面跳转（现有方案）。
3. **T52 搜索下拉**：需确认后端 `pageRoom` 接口支持 `roomCode` 模糊搜索（当前已支持 `like` 查询）。
4. **T53 图标**：删除全局注册前必须确认所有图标都有本地 import，否则部分图标不显示。建议实施后逐页面检查。
5. **EX1 事务**：`ownerService.create()` 若自身已有 `@Transactional`，TransactionTemplate 的 REQUIRES_NEW 会挂起外层事务创建新事务，行为正确。若无需新事务可改为 REQUIRED。
6. **向后兼容**：AuthInterceptor 同时支持 Cookie 和 Authorization 头，过渡期内两种方式均可。
