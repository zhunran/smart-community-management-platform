# 阶段四操作手册：业主端移动化（Vant 4）

> 配套文档：[升级改造计划书](./upgrade-plan.md)
> 编制日期：2026-08-19
> 阶段范围：业主端 UI 从 Element Plus（PC）迁移至 Vant 4（移动端），layout 适配、viewport 适配、底部 TabBar 导航
> 预计影响：`property-owner-web` 全面改造（10 个页面 + 入口文件 + 样式体系），管理端 `property-admin-web` 不受影响
> 前置条件：阶段一～三已完成（后端 API 不变，前端仅改 UI 层）

---

## 目录

- [1. 变更总览](#1-变更总览)
- [2. 环境准备](#2-环境准备)
- [3. Vant 引入与移动端适配（步骤 1-4）](#3-vant-引入与移动端适配步骤-1-4)
- [4. 布局改造（步骤 5-6）](#4-布局改造步骤-5-6)
- [5. 页面改造（步骤 7-16）](#5-页面改造步骤-7-16)
- [6. 编译与验证](#6-编译与验证)
- [7. 验收标准](#7-验收标准)
- [附录 A：完整文件变更清单](#附录-a完整文件变更清单)
- [附录 B：Element Plus → Vant 组件映射表](#附录-belement-plus--vant-组件映射表)
- [附录 C：TabBar 路由设计](#附录-ctabbar-路由设计)

---

## 1. 变更总览

### 1.1 技术引入矩阵

| 组件 | 当前版本 | 目标版本 | 说明 |
|------|---------|---------|------|
| Vant | 无 | **4.x** | 移动端 UI 组件库，轻量、按需引入 |
| postcss-px-to-viewport | 无 | **1.1.1** | px → vw 自动转换，适配不同屏幕宽度 |
| Element Plus | 2.9.7 | **保留**（管理端用） | 业主端不再使用 Element Plus 组件 |

### 1.2 功能变更矩阵

| 功能 | 当前状态 | 目标状态 |
|------|---------|---------|
| UI 组件库 | Element Plus（PC 风格） | Vant 4（移动端风格） |
| 布局 | 无统一布局，各页面独立 header | 底部 TabBar + 统一 NavBar |
| 视口适配 | 固定 px | px → vw 自动转换（375px 设计稿） |
| 导航方式 | 各页面独立返回按钮 | TabBar 主 tab + 子页面 NavBar 返回 |
| 登录页 | Element Plus 表单 | Vant 表单 + 移动端适配 |
| AI 客服 | 原生 HTML/CSS（已移动端友好） | 不变（无需改造） |

### 1.3 变更文件清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | [package.json](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/package.json)（新增 vant + postcss-px-to-viewport） |
| 新增 | `postcss.config.cjs`（postcss-px-to-viewport 配置） |
| 修改 | [src/main.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/main.ts)（移除 Element Plus 全局注册，按需引入 Vant） |
| 修改 | [src/App.vue](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/App.vue)（新增布局逻辑） |
| 新增 | `src/components/TabBarLayout.vue`（底部 TabBar 布局组件） |
| 修改 | [src/router/index.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/router/index.ts)（路由 meta 新增 tabBar 标记） |
| 修改 | `src/views/login/LoginView.vue`（Element Plus → Vant） |
| 修改 | `src/views/home/HomeView.vue`（Element Plus → Vant + TabBar 页） |
| 修改 | `src/views/bills/BillsView.vue`（Element Plus → Vant + TabBar 页） |
| 修改 | `src/views/bills/BillDetailView.vue`（Element Plus → Vant） |
| 修改 | `src/views/payment/PaymentView.vue`（Element Plus → Vant + TabBar 页） |
| 修改 | `src/views/payment/PaymentSuccessView.vue`（Element Plus → Vant） |
| 修改 | `src/views/records/RecordsView.vue`（Element Plus → Vant） |
| 修改 | `src/views/rooms/RoomsView.vue`（Element Plus → Vant） |
| 修改 | `src/views/profile/ProfileView.vue`（Element Plus → Vant + TabBar 页） |
| 不变 | `src/views/chat/ChatView.vue`（已使用原生 HTML/CSS，移动端友好） |

---

## 2. 环境准备

### 2.1 确认 Node.js 版本

```powershell
node -v  # 需 >= 18
```

### 2.2 确认当前项目可正常编译

```powershell
cd property-owner-web
npm run build
```

确保改造前项目编译通过，便于改造后对比。

---

## 3. Vant 引入与移动端适配（步骤 1-4）

### 步骤 1：安装依赖

**文件**：[package.json](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/package.json)

在 `property-owner-web` 目录下执行：

```powershell
npm install vant@4
npm install -D postcss-px-to-viewport
```

执行后 `package.json` 应新增：

```json
{
  "dependencies": {
    "vant": "^4.9.0"
  },
  "devDependencies": {
    "postcss-px-to-viewport": "^1.1.1"
  }
}
```

### 步骤 2：配置 postcss-px-to-viewport

**新建文件**：`property-owner-web/postcss.config.cjs`

> 注意：使用 `.cjs` 扩展名，因为 Vite 6 的 postcss 配置需要使用 CommonJS 格式。

```js
module.exports = {
  plugins: {
    'postcss-px-to-viewport': {
      viewportWidth: 375,       // 设计稿宽度（iPhone 6/7/8 标准）
      unitPrecision: 5,         // 转换后保留小数位数
      viewportUnit: 'vw',       // 转换目标单位
      selectorBlackList: [],    // 不转换的选择器
      minPixelValue: 1,         // 小于 1px 不转换
      mediaQuery: false,        // 媒体查询中不转换
      exclude: [],              // 排除文件（正则）
    },
  },
}
```

**设计说明**：
- `viewportWidth: 375`：以 iPhone 6/7/8 为基准，编译时 `16px` → `4.267vw`（16/375*100）
- 所有页面中的 `px` 值会自动转换为 `vw`，无需手动修改样式
- 边框 `1px` 不转换（`minPixelValue: 1`），防止边框过细

### 步骤 3：改造 main.ts（移除 Element Plus，引入 Vant）

**文件**：[src/main.ts](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/main.ts)

**改造前**（当前代码）：

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
```

**改造后**：

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Vant 4 按需引入样式
import 'vant/lib/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

**关键变更**：
- 移除 `ElementPlus` 全局注册和 CSS 引入
- 移除 Element Plus Icons 全局注册
- 新增 `vant/lib/index.css`（Vant 4 基础样式）
- Vant 组件采用**按需自动引入**方式（见下方步骤 3.1，必须配置，否则 `van-*` 组件无法解析、页面点击全部失效）

### 步骤 3.1：配置 unplugin-vue-components 自动按需引入（关键）

> **注意**：仅引入 `vant/lib/index.css` 并不会注册组件。视图中的 `<van-*>` 组件必须能被解析，否则渲染为空元素、所有交互失效。Vant 官方推荐使用 `unplugin-vue-components` + `VantResolver` 实现模板内自动按需引入。

**安装依赖**：

```powershell
npm install -D unplugin-vue-components
```

**文件**：[vite.config.ts](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/vite.config.ts)

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // Vant 组件按需自动注册（van-* 组件自动引入对应组件与样式）
    Components({
      resolvers: [VantResolver()],
    }),
  ],
  ...
})
```

**说明**：
- 该插件会自动扫描模板中的 `<van-*>` 标签，按需引入对应组件与样式，并生成 `components.d.ts` 提供 TS 类型
- 函数式调用（`showToast`、`showConfirmDialog`、`showDialog` 等）仍需在 `<script setup>` 中显式 `import { showToast } from 'vant'`
- 完成后重启 dev server，页面出现 `Failed to resolve component: van-xxx` 警告即说明解析成功

### 步骤 4：改造 App.vue（布局入口）

**文件**：[src/App.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/App.vue)

**改造前**：

```vue
<template>
  <router-view />
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
</style>
```

**改造后**：

```vue
<template>
  <router-view />
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app {
  height: 100%;
  font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  /* 移动端字体最小 14px，禁止缩放 */
  -webkit-text-size-adjust: 100%;
  /* 禁止长按弹出菜单 */
  -webkit-touch-callout: none;
  /* 点击高亮 */
  -webkit-tap-highlight-color: transparent;
}
</style>
```

---

## 4. 布局改造（步骤 5-6）

### 步骤 5：新增 TabBar 布局组件

**新建文件**：`property-owner-web/src/components/TabBarLayout.vue`

```vue
<template>
  <div class="tab-layout">
    <div class="tab-content">
      <router-view />
    </div>
    <van-tabbar
      v-model="activeTab"
      :fixed="true"
      :border="true"
      active-color="#1989fa"
      inactive-color="#7d7e80"
      @change="onTabChange"
    >
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/bills">账单</van-tabbar-item>
      <van-tabbar-item icon="balance-o" to="/payment">缴费</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeTab = ref(0)

const tabMap: Record<string, number> = {
  '/': 0,
  '/bills': 1,
  '/payment': 2,
  '/profile': 3,
}

watch(
  () => route.path,
  (path) => {
    if (tabMap[path] !== undefined) {
      activeTab.value = tabMap[path]
    }
  },
  { immediate: true }
)

function onTabChange(index: number) {
  activeTab.value = index
}
</script>

<style scoped>
.tab-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.tab-content {
  flex: 1;
  overflow-y: auto;
  /* 留出底部 TabBar 高度（50px = 约 13.33vw） */
  padding-bottom: 50px;
}
</style>
```

**设计说明**：
- 4 个 Tab：首页、账单、缴费、我的
- TabBar 使用 `fixed` 定位，始终在底部
- `tab-content` 预留底部 padding 防止内容被遮挡
- 图标使用 Vant 内置图标（`home-o`、`orders-o`、`balance-o`、`user-o`），无需额外引入图标库

### 步骤 6：路由调整（meta 标记 + 布局嵌套）

**文件**：[src/router/index.ts](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/router/index.ts)

**改造后**：

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useOwnerStore } from '@/stores/owner'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  // TabBar 布局下的主页面
  {
    path: '/',
    component: () => import('@/components/TabBarLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'bills',
        component: () => import('@/views/bills/BillsView.vue'),
        meta: { title: '我的账单' },
      },
      {
        path: 'payment',
        component: () => import('@/views/payment/PaymentView.vue'),
        meta: { title: '在线缴费' },
      },
      {
        path: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人信息' },
      },
    ],
  },
  // 子页面（无 TabBar，有返回按钮）
  {
    path: '/bills/:id',
    component: () => import('@/views/bills/BillDetailView.vue'),
    meta: { requiresAuth: true, title: '账单明细' },
  },
  {
    path: '/payment/success/:paymentNo',
    component: () => import('@/views/payment/PaymentSuccessView.vue'),
    meta: { requiresAuth: true, title: '支付成功' },
  },
  {
    path: '/records',
    component: () => import('@/views/records/RecordsView.vue'),
    meta: { requiresAuth: true, title: '支付记录' },
  },
  {
    path: '/rooms',
    component: () => import('@/views/rooms/RoomsView.vue'),
    meta: { requiresAuth: true, title: '我的房屋' },
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/chat/ChatView.vue'),
    meta: { requiresAuth: true, title: 'AI 客服' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const store = useOwnerStore()
  if (to.meta.requiresAuth === false) { next(); return }
  if (to.meta.requiresAuth && !store.isLoggedIn()) { next('/login'); return }
  next()
})

export default router
```

**关键变更**：
- 4 个主页面（首页/账单/缴费/我的）作为 `TabBarLayout` 的 children
- 子页面（BillDetail、PaymentSuccess、Records、Rooms、Chat）保持独立路由，无 TabBar
- 登录页 `/login` 保持独立
- 路由守卫逻辑不变

---

## 5. 页面改造（步骤 7-16）

### 改造原则

1. **Element Plus 组件 → Vant 组件**：对照 [附录 B](#附录-belement-plus--vant-组件映射表) 逐一替换
2. **页面结构**：
   - TabBar 页面（首页/账单/缴费/我的）：移除顶部 header，由 TabBar 统一导航
   - 子页面（明细/支付成功/记录/房屋/聊天）：NavBar 顶部返回栏
3. **样式**：postcss-px-to-viewport 自动将 px 转为 vw，无需手动改样式
4. **Vant 组件按需自动引入**：由 `unplugin-vue-components` + `VantResolver` 在 vite.config.ts 中统一配置，模板 `<van-*>` 自动解析；函数式 API（showToast 等）在 script 中显式 import

---

### 步骤 7：改造登录页 LoginView.vue

**文件**：[src/views/login/LoginView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/login/LoginView.vue)

**改造后**：

```vue
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">🏠</div>
        <h2>智慧物业管理平台</h2>
        <p>业主端</p>
      </div>

      <van-form @submit="handleLogin">
        <van-cell-group inset>
          <van-field
            v-model="loginForm.phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            maxlength="11"
            type="tel"
            :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1\d{10}$/, message: '手机号格式不正确' }]"
          />
          <van-field
            v-model="loginForm.password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            type="password"
            :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }]"
          />
          <van-field
            v-model="loginForm.captcha"
            name="captcha"
            label="验证码"
            placeholder="请输入验证码"
            maxlength="5"
            :rules="[{ required: true, message: '请输入验证码' }]"
          >
            <template #button>
              <img
                :src="captchaUrl"
                class="captcha-img"
                alt="验证码"
                title="点击刷新"
                @click="refreshCaptcha"
              />
            </template>
          </van-field>
        </van-cell-group>

        <div style="margin: 16px">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="loading"
            loading-text="登录中..."
          >
            登 录
          </van-button>
        </div>
      </van-form>

      <div class="login-footer">
        <span>默认密码为手机号后 6 位</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showNotify } from 'vant'
import { useOwnerStore } from '@/stores/owner'

const router = useRouter()
const ownerStore = useOwnerStore()

const loading = ref(false)
const captchaUrl = ref('')

function refreshCaptcha() {
  captchaUrl.value = '/api/owner/auth/captcha?t=' + Date.now()
}

const loginForm = reactive({
  phone: '',
  password: '',
  captcha: '',
})

async function handleLogin() {
  loading.value = true
  try {
    await ownerStore.login({
      phone: loginForm.phone,
      password: loginForm.password,
      captcha: loginForm.captcha,
    })
    router.push('/')
  } catch {
    refreshCaptcha()
    loginForm.captcha = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.login-card {
  width: 85%;
  padding: 32px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.login-header h2 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 4px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
}

.captcha-img {
  height: 40px;
  width: 100px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 16px;
}
</style>
```

**关键变更**：
- `el-form` → `van-form` + `van-cell-group` + `van-field`
- `el-input` → `van-field`（Vant 内置表单校验）
- `el-button` → `van-button`
- 验证码 `img` 放在 `van-field` 的 `#button` 插槽中
- 移除 Element Plus Icons 的 `Iphone`、`Lock`、`Key` 等图标引用
- 错误提示用 `showNotify`（Vant 内置）替代 `ElMessage`

---

### 步骤 8：改造首页 HomeView.vue

**文件**：[src/views/home/HomeView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/home/HomeView.vue)

**改造后**：

```vue
<template>
  <div class="home">
    <!-- 顶部欢迎区 -->
    <div class="home-header">
      <div class="header-left">
        <span class="header-title">智慧物业</span>
      </div>
      <div class="header-right" @click="showMenu = true">
        <span class="user-name">{{ ownerStore.ownerName }}</span>
        <van-icon name="arrow-down" size="12" />
      </div>
    </div>

    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <h3>欢迎回来</h3>
      <p class="welcome-name">{{ ownerStore.ownerName }}</p>
      <p class="welcome-phone">{{ maskPhone(ownerStore.phone) }}</p>
    </div>

    <!-- 功能入口 -->
    <van-grid :column-num="3" :border="false" :gutter="12">
      <van-grid-item icon="orders-o" text="我的账单" @click="router.push('/bills')" />
      <van-grid-item icon="balance-o" text="在线缴费" @click="router.push('/payment')" />
      <van-grid-item icon="records-o" text="支付记录" @click="router.push('/records')" />
      <van-grid-item icon="home-o" text="我的房屋" @click="router.push('/rooms')" />
      <van-grid-item icon="user-o" text="个人信息" @click="router.push('/profile')" />
      <van-grid-item icon="service-o" text="AI 客服" @click="router.push('/chat')" />
    </van-grid>

    <!-- 下拉菜单 -->
    <van-action-sheet
      v-model:show="showMenu"
      :actions="menuActions"
      cancel-text="取消"
      @select="onMenuSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useOwnerStore } from '@/stores/owner'
import { showConfirmDialog } from 'vant'

const router = useRouter()
const ownerStore = useOwnerStore()
const showMenu = ref(false)

const menuActions = [
  { name: '个人信息', value: 'profile' },
  { name: '我的房屋', value: 'rooms' },
  { name: '退出登录', value: 'logout' },
]

function onMenuSelect(action: { name: string; value: string }) {
  showMenu.value = false
  if (action.value === 'logout') {
    showConfirmDialog({ title: '提示', message: '确认退出登录？' })
      .then(async () => { await ownerStore.logout(); router.push('/login') })
      .catch(() => {})
  } else {
    router.push(`/${action.value}`)
  }
}

function maskPhone(p: string) {
  if (!p || p.length < 11) return p
  return p.substring(0, 3) + '****' + p.substring(7)
}
</script>

<style scoped>
.home {
  background: #f5f7fa;
  min-height: 100%;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.welcome-card {
  margin: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  text-align: center;
}

.welcome-card h3 {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.welcome-name {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 4px;
}

.welcome-phone {
  font-size: 13px;
  opacity: 0.8;
}
</style>
```

**关键变更**：
- 移除顶部 Element Plus 的 `el-dropdown`，改用 Vant 的 `van-action-sheet` 下拉菜单
- 功能入口从 `div.func-grid` 改为 `van-grid`（Vant 九宫格）
- 不再需要返回按钮（TabBar 页面）
- 退出登录用 `showConfirmDialog` 替代 `ElMessageBox.confirm`

---

### 步骤 9：改造账单列表页 BillsView.vue

**文件**：[src/views/bills/BillsView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/bills/BillsView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <van-tabs v-model:active="activeTab" @change="loadBills" sticky>
      <van-tab title="未缴费" name="unpaid" />
      <van-tab title="已缴清" name="paid" />
      <van-tab title="历史账单" name="all" />
    </van-tabs>

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="bill in bills"
          :key="bill.id"
          :title="bill.billPeriod"
          :label="bill.buildingName + ' ' + bill.roomCode"
          is-link
          @click="goDetail(bill.id)"
        >
          <template #value>
            <div class="bill-amount">
              <span class="amount">&yen;{{ bill.totalAmount }}</span>
            </div>
          </template>
          <template #extra>
            <van-tag :type="statusTag(bill.status)" size="medium">
              {{ statusName(bill.status) }}
            </van-tag>
          </template>
        </van-cell>
        <van-empty v-if="bills.length === 0" description="暂无账单" />
      </template>

      <van-pagination
        v-if="total > size"
        v-model="current"
        :total-items="total"
        :page-size="size"
        mode="simple"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBillPage } from '@/api/bill'
import type { BillVO } from '@/api/bill'

const router = useRouter()
const activeTab = ref('unpaid')
const bills = ref<BillVO[]>([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)

function goDetail(id: string) { router.push(`/bills/${id}`) }

const statusMap: Record<number, string> = { 0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期' }
function statusName(s: number) { return statusMap[s] || '未知' }
function statusTag(s: number) {
  return s === 2 ? 'success' : s === 5 ? 'danger' : s === 1 ? 'warning' : 'primary'
}

function getStatusFilter(): number | undefined {
  if (activeTab.value === 'unpaid') return undefined
  if (activeTab.value === 'paid') return 2
  return undefined
}

async function loadBills() {
  loading.value = true
  current.value = 1
  try {
    const res = await getBillPage({ current: current.value, size: size.value, status: getStatusFilter() })
    const data = res.data as any
    bills.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function onPageChange(p: number) {
  current.value = p
  loading.value = true
  try {
    const res = await getBillPage({ current: current.value, size: size.value, status: getStatusFilter() })
    const data = res.data as any
    bills.value = data.records || []
  } finally { loading.value = false }
}

onMounted(loadBills)
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 16px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.bill-amount {
  text-align: right;
}

.amount {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}
</style>
```

**关键变更**：
- `el-tabs` → `van-tabs`（`sticky` 吸顶）
- `el-card` → `van-cell`（Vant 单元格列表，移动端更自然）
- `el-tag` → `van-tag`
- `el-pagination` → `van-pagination`（`mode="simple"` 简洁模式）
- `el-empty` → `van-empty`
- `v-loading` → `van-loading`
- 移除顶部 header（TabBar 页面，无需返回按钮）

---

### 步骤 10：改造账单明细页 BillDetailView.vue

**文件**：[src/views/bills/BillDetailView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/bills/BillDetailView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <van-nav-bar title="账单明细" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body" v-if="detail">
      <van-loading v-if="loading" class="loading-center" />

      <van-cell-group inset title="基本信息">
        <van-cell title="账单周期" :value="detail.billPeriod" />
        <van-cell title="楼栋房号" :value="detail.buildingName + ' ' + detail.roomCode" />
        <van-cell title="账单编号" :value="detail.billNo" />
      </van-cell-group>

      <van-cell-group inset title="费用明细" v-if="detail.feeItems && detail.feeItems.length > 0">
        <van-cell
          v-for="(item, idx) in detail.feeItems"
          :key="idx"
          :title="item.feeItemName"
          :label="'基数：' + item.baseValue + ' × 单价：' + item.unitPrice"
        >
          <template #value>
            <span class="fee-amount">&yen;{{ item.amount }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset title="金额汇总">
        <van-cell title="应缴金额" :value="'¥' + detail.totalAmount" />
        <van-cell title="已缴金额" :value="'¥' + (detail.paidAmount || 0)" />
        <van-cell v-if="detail.lateFee" title="逾期罚息" :value="'¥' + detail.lateFee" />
      </van-cell-group>

      <div class="pay-btn-wrapper" v-if="detail.status !== 2">
        <van-button
          type="primary"
          block
          round
          size="large"
          @click="router.push('/payment?billId=' + detail.id)"
        >
          立即缴费
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBillDetail } from '@/api/bill'
import type { BillDetailVO } from '@/api/bill'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<BillDetailVO>()

function goBack() { router.back() }

onMounted(async () => {
  loading.value = true
  try {
    const id = route.params.id as string
    const res = await getBillDetail(id)
    detail.value = res.data as BillDetailVO
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.fee-amount {
  font-weight: bold;
  color: #e6a23c;
}

.pay-btn-wrapper {
  padding: 24px 16px;
}
</style>
```

**关键变更**：
- `el-card` + `el-descriptions` → `van-cell-group` + `van-cell`（Vant 单元格组）
- 顶部 header → `van-nav-bar`（Vant 导航栏，自带返回按钮）
- `el-button` → `van-button`
- `el-empty` → `van-empty`

---

### 步骤 11：改造在线缴费页 PaymentView.vue

**文件**：[src/views/payment/PaymentView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/payment/PaymentView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <div class="page-body">
      <van-loading v-if="billsLoading" class="loading-center" />

      <van-cell-group inset title="选择要缴费的账单（点击切换）">
        <van-cell
          v-for="bill in bills"
          :key="bill.id"
          :title="bill.billPeriod"
          :label="bill.buildingName + ' ' + bill.roomCode"
          @click="toggleBill(bill)"
        >
          <template #icon>
            <van-checkbox
              :model-value="selectedIds.has(bill.id)"
              @click.stop
              style="margin-right: 8px"
            />
          </template>
          <template #value>
            <span class="bill-amount">
              &yen;{{ (bill.totalAmount - bill.paidAmount + (bill.lateFee || 0)).toFixed(2) }}
            </span>
          </template>
          <template #extra>
            <van-tag :type="statusTag(bill.status)" size="medium">
              {{ statusName(bill.status) }}
            </van-tag>
          </template>
        </van-cell>
        <van-empty v-if="!billsLoading && bills.length === 0" description="暂无待缴费账单" />
      </van-cell-group>

      <div v-if="selectedIds.size > 0" class="summary-card">
        <van-cell-group inset>
          <van-cell title="选择账单" :value="selectedIds.size + ' 笔'" />
          <van-cell title="合计金额">
            <template #value>
              <span class="summary-amount">&yen;{{ totalAmount.toFixed(2) }}</span>
            </template>
          </van-cell>
        </van-cell-group>
        <div class="submit-wrapper">
          <van-button
            type="primary"
            block
            round
            size="large"
            :loading="payLoading"
            @click="submitPay"
          >
            确认支付
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showNotify, showConfirmDialog } from 'vant'
import { getBillPage } from '@/api/bill'
import { createPayOrder } from '@/api/payment'
import type { BillVO } from '@/api/bill'

const route = useRoute()
const router = useRouter()
const billsLoading = ref(false)
const payLoading = ref(false)
const bills = ref<BillVO[]>([])
const selectedIds = ref(new Set<string>())

const totalAmount = computed(() => {
  let total = 0
  for (const bill of bills.value) {
    if (selectedIds.value.has(bill.id)) {
      total += bill.totalAmount - bill.paidAmount + (bill.lateFee || 0)
    }
  }
  return total
})

const statusMap: Record<number, string> = { 0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期' }
function statusName(s: number) { return statusMap[s] || '未知' }
function statusTag(s: number) {
  return s === 2 ? 'success' : s === 5 ? 'danger' : s === 1 ? 'warning' : 'primary'
}

function toggleBill(bill: BillVO) {
  if (selectedIds.value.has(bill.id)) {
    selectedIds.value.delete(bill.id)
  } else {
    selectedIds.value.add(bill.id)
  }
}

async function submitPay() {
  if (selectedIds.value.size === 0) { showNotify({ type: 'warning', message: '请选择至少一个账单' }); return }
  if (selectedIds.value.size > 1) { showNotify({ type: 'warning', message: '暂仅支持单笔账单支付' }); return }
  const billId = Array.from(selectedIds.value)[0]
  try {
    await showConfirmDialog({
      title: '支付确认',
      message: `确认支付 ¥${totalAmount.value.toFixed(2)}？`,
    })
  } catch { return }
  payLoading.value = true
  try {
    const res = await createPayOrder({ billId, paymentMethod: 1, payerName: '业主' })
    const data = res.data as any
    if (data.payFormHtml) {
      const container = document.createElement('div')
      container.innerHTML = data.payFormHtml
      const form = container.querySelector('form')
      if (form) {
        document.body.appendChild(form)
        form.submit()
        return
      }
    }
    selectedIds.value.clear()
    router.push('/records')
  } catch (e: any) {
    showNotify({ type: 'danger', message: e?.msg || e?.message || '支付失败' })
  } finally { payLoading.value = false }
}

onMounted(async () => {
  billsLoading.value = true
  try {
    const res = await getBillPage({ current: 1, size: 50 })
    const data = res.data as any
    bills.value = (data.records || []).filter(
      (b: BillVO) => b.status !== 2 && b.status !== 3 && b.status !== 4
    )
    const billId = route.query.billId
    if (billId) {
      const found = bills.value.find(b => b.id === String(billId))
      if (found) { selectedIds.value.add(found.id) }
    }
  } finally { billsLoading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.bill-amount {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}

.summary-card {
  margin-top: 12px;
}

.summary-amount {
  font-size: 20px;
  font-weight: bold;
  color: #e6a23c;
}

.submit-wrapper {
  padding: 16px;
}
</style>
```

**关键变更**：
- `el-checkbox` → `van-checkbox`（放在 `van-cell` 的 `#icon` 插槽中）
- `el-card` → `van-cell-group` + `van-cell`
- `el-button` → `van-button`
- `ElMessage` → `showNotify`
- `ElMessageBox.confirm` → `showConfirmDialog`
- 移除顶部 header（TabBar 页面）

---

### 步骤 12：改造支付成功页 PaymentSuccessView.vue

**文件**：[src/views/payment/PaymentSuccessView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/payment/PaymentSuccessView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <van-nav-bar title="支付成功" left-text="返回" left-arrow @click-left="goBack" />

    <div class="success-body">
      <van-icon name="checked" size="64" color="#07c160" />
      <h2>支付成功</h2>
      <p class="payment-no">支付单号：{{ paymentNo }}</p>
      <div class="actions">
        <van-button type="primary" block round @click="router.push('/records')">查看支付记录</van-button>
        <van-button plain block round style="margin-top: 12px" @click="router.push('/')">返回首页</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()

const paymentNo = computed(() => route.params.paymentNo as string)
function goBack() { router.back() }
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}

.success-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 24px;
  text-align: center;
}

.success-body h2 {
  margin: 16px 0 8px;
  font-size: 20px;
}

.payment-no {
  font-size: 13px;
  color: #909399;
  margin-bottom: 32px;
}

.actions {
  width: 100%;
  max-width: 300px;
}
</style>
```

**关键变更**：
- 顶部 header → `van-nav-bar`
- 绿色对勾改用 `van-icon name="checked"`（Vant 内置图标）
- `el-button` → `van-button`

---

### 步骤 13：改造支付记录页 RecordsView.vue

**文件**：[src/views/records/RecordsView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/records/RecordsView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <van-nav-bar title="支付记录" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="r in records"
          :key="r.id"
          :title="'支付单号：' + r.paymentNo"
          :label="r.billPeriod + ' ' + (r.billNo || '')"
          is-link
          @click="showDetail(r)"
        >
          <template #value>
            <div class="record-amount">&yen;{{ r.paymentAmount }}</div>
          </template>
          <template #extra>
            <van-tag :type="payStatusTag(r.paymentStatus)" size="medium">
              {{ r.paymentStatusName }}
            </van-tag>
          </template>
        </van-cell>
        <van-empty v-if="records.length === 0" description="暂无支付记录" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPaymentPage } from '@/api/payment'
import type { PaymentOrderVO } from '@/api/payment'
import { showDialog } from 'vant'

const router = useRouter()
const loading = ref(false)
const records = ref<PaymentOrderVO[]>([])
function goBack() { router.back() }

function payStatusTag(s: number) {
  return s === 2 ? 'success' : s === 3 ? 'danger' : s === 0 ? 'warning' : 'primary'
}

function showDetail(r: PaymentOrderVO) {
  showDialog({
    title: '支付详情',
    message: `支付单号：${r.paymentNo}<br>金额：¥${r.paymentAmount}<br>方式：${r.paymentMethodName}<br>流水号：${r.transactionId || '-'}<br>状态：${r.paymentStatusName}<br>时间：${r.paymentTime || r.createTime}`,
    allowHtml: true,
    confirmButtonText: '知道了',
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getPaymentPage({ current: 1, size: 50 })
    const data = res.data as any
    records.value = data.records || []
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.record-amount {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}
</style>
```

**关键变更**：
- `el-card` → `van-cell`
- `el-tag` → `van-tag`
- `ElMessageBox.alert` → `showDialog`（Vant 对话框）
- 顶部 header → `van-nav-bar`

---

### 步骤 14：改造我的房屋页 RoomsView.vue

**文件**：[src/views/rooms/RoomsView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/rooms/RoomsView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <van-nav-bar title="我的房屋" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="room in rooms"
          :key="room.id"
          :title="room.roomName || room.roomCode"
          :label="room.buildingName"
        >
          <template #extra>
            <van-tag :type="room.isPrimary === 1 ? 'danger' : 'primary'" size="medium">
              {{ room.isPrimary === 1 ? '主要' : '其他' }}
            </van-tag>
            <span class="relation">{{ room.relationType === 1 ? '业主' : room.relationType === 2 ? '家属' : '租客' }}</span>
          </template>
        </van-cell>
        <van-empty v-if="rooms.length === 0" description="暂无房屋信息" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRooms } from '@/api/profile'
import type { OwnerRoomVO } from '@/api/profile'

const router = useRouter()
const loading = ref(false)
const rooms = ref<OwnerRoomVO[]>([])
function goBack() { router.back() }

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyRooms()
    rooms.value = res.data as OwnerRoomVO[]
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.relation {
  font-size: 12px;
  color: #67c23a;
  margin-left: 8px;
}
</style>
```

---

### 步骤 15：改造个人信息页 ProfileView.vue

**文件**：[src/views/profile/ProfileView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/profile/ProfileView.vue)

**改造后**：

```vue
<template>
  <div class="page">
    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-if="profile">
        <!-- 头像区 -->
        <div class="profile-avatar">
          <van-image
            round
            width="72"
            height="72"
            src="https://img.yzcdn.cn/vant/cat.jpeg"
            fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAMAAACahl6sAAAAZlBMVEX///8AAAD29vb5+fn8/Pz4+Pj6+vr7+/v19fXz8/Pu7u7v7+/x8fHt7e3s7Ozo6Ojl5eXk5OTi4uLg4ODd3d3b29va2trY2NjW1tbU1NTS0tLQ0NDOzs7MzMzJycnHx8fFxcXDw8PBwcG/v7+9vb28vLyrq6uQPmdHAAABGklEQVR4nO3c2W6DMBCFYRdCQvbVkO1d3v8pSxVVRYjF8j8j8V1F1/ftGJtgHJNlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADwX0n/L0CCNyQZkGR4RxsEJbH0K0qC54Y8QNLqwQMNyR0YkGR4x2sNwqYkA5IMT5csQEI2JZkfJhmeAuQ6kkyPkQxPU5JBT5LhA5LhAWnKgCRDkuFJXQqQpEeS4QmS+0E6IhkeIAkAAAAAAADg+5weHt9fH4+9f30+9r311fF4W3kRkjbO5y+3nj9ehx9qLldvsu3y9/Aqc7N6k6XJ2+Qj3iFv8nHth7xD3iTv8gb5kH+Q2yRvk3f5gHyN3CJ5m7xLnst3y7m8S9Y1b0vWydv4B5J3eZvSm7KuUZq0TtpGXmnWae2srbe2ztq7a2+u6bO6bqO6r+oGq7ur7ri65+rOq7uv7sC6j+terLux7sy6F+tuLgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADY4BeA/xMa0dBGDQAAAABJRU5ErkJggg=="
          />
          <div class="profile-name">{{ profile.ownerName }}</div>
        </div>

        <van-cell-group inset>
          <van-cell title="姓名" :value="profile.ownerName" />
          <van-cell title="手机号" :value="profile.phone" />
          <van-cell title="证件号" :value="profile.idCardNo" />
          <van-cell title="性别" :value="profile.gender === 1 ? '男' : profile.gender === 2 ? '女' : '未知'" />
          <van-cell title="邮箱" :value="profile.email || '未设置'" />
          <van-cell title="业主类型" :value="profile.ownerType === 1 ? '个人' : profile.ownerType === 2 ? '公司' : '共有'" />
          <van-cell title="注册时间" :value="profile.registerTime" />
          <van-cell title="最近登录" :value="profile.lastLoginTime || '首次登录'" />
        </van-cell-group>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getProfile } from '@/api/profile'
import type { OwnerDetailVO } from '@/api/profile'

const loading = ref(false)
const profile = ref<OwnerDetailVO>()

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data as OwnerDetailVO
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.profile-avatar {
  text-align: center;
  padding: 24px 0;
  background: #fff;
  margin-bottom: 12px;
}

.profile-name {
  margin-top: 8px;
  font-size: 18px;
  font-weight: bold;
}
</style>
```

**关键变更**：
- `el-descriptions` → `van-cell-group` + `van-cell`
- `el-avatar` → `van-image`（round 圆形）
- 顶部 header 移除（TabBar 页面，无需返回按钮）

---

### 步骤 16：确认 ChatView.vue 不变

**文件**：[src/views/chat/ChatView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/chat/ChatView.vue)

AI 客服页面在阶段三已使用原生 HTML/CSS 开发，自身已是移动端友好的设计（max-width: 600px + flex 布局），无需改造。

---

## 6. 编译与验证

### 6.1 安装依赖

```powershell
cd property-owner-web
npm install
```

### 6.2 编译验证

```powershell
npm run build
```

预期输出：`vue-tsc -b && vite build` 均通过，无 TypeScript 错误。

**常见编译问题**：

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `Cannot find module 'vant'` | 未安装依赖 | 执行 `npm install` |
| `Failed to resolve component: van-xxx` | 未配置自动按需引入 | 安装 `unplugin-vue-components` 并在 vite.config.ts 配置 `VantResolver`（见步骤 3.1） |
| `postcss-px-to-viewport` 不生效 | 配置文件未被识别 | 确认文件名为 `postcss.config.cjs`（不是 `.js` 或 `.ts`） |
| Element Plus 残留引用 | 某个页面忘记替换 | 全局搜索 `el-` 前缀，确认全部替换 |

### 6.3 启动验证

```powershell
npm run dev
```

浏览器访问 `http://localhost:5273`，按以下清单验证：

1. **登录页**：Vant 表单样式正常，手机号/密码/验证码输入正常，登录跳转正常
2. **首页**：底部 TabBar 显示正常，点击各 Tab 切换正常，功能入口点击跳转正常
3. **账单页**：Tab 切换正常，列表滚动正常，点击进入明细正常
4. **缴费页**：账单选择正常，合计金额计算正确，支付跳转正常
5. **我的页**：个人信息展示正常
6. **子页面**：NavBar 返回按钮正常，无 TabBar
7. **移动端适配**：使用 Chrome DevTools 切换到移动端视图（375px 宽度），页面比例正常，无横向滚动条

### 6.4 回归验证

改造后确保以下功能不受影响：

- [ ] 登录/登出正常
- [ ] 账单列表/明细正常
- [ ] 在线缴费正常（支付宝收银台跳转）
- [ ] 支付记录正常
- [ ] 我的房屋正常
- [ ] 个人信息正常
- [ ] AI 客服正常（SSE 流式对话）
- [ ] Token 刷新正常（401 自动重试）

---

## 7. 验收标准

- [ ] `npm run build` 编译通过，无 TypeScript 错误
- [ ] 所有页面使用 Vant 4 组件，无 Element Plus 组件残留
- [ ] 底部 TabBar 4 个 Tab（首页/账单/缴费/我的）正常切换
- [ ] 子页面 NavBar 返回按钮正常
- [ ] 登录页 Vant 表单校验正常
- [ ] postcss-px-to-viewport 自动转换 px → vw 生效
- [ ] 移动端 Chrome DevTools 375px 宽度下页面显示正常
- [ ] AI 客服页面功能不受影响
- [ ] 管理端 `property-admin-web` 不受影响（独立项目）

---

## 附录 A：完整文件变更清单

| 操作 | 文件 | 变更内容 |
|------|------|---------|
| 修改 | `package.json` | 新增 `vant`、`postcss-px-to-viewport` |
| 新增 | `postcss.config.cjs` | postcss-px-to-viewport 配置 |
| 修改 | `src/main.ts` | 移除 Element Plus 全局注册，引入 Vant CSS |
| 修改 | `src/App.vue` | 新增移动端样式优化 |
| 新增 | `src/components/TabBarLayout.vue` | 底部 TabBar 布局组件 |
| 修改 | `src/router/index.ts` | 路由结构调整（TabBar 嵌套路由） |
| 修改 | `src/views/login/LoginView.vue` | Element Plus → Vant |
| 修改 | `src/views/home/HomeView.vue` | Element Plus → Vant |
| 修改 | `src/views/bills/BillsView.vue` | Element Plus → Vant |
| 修改 | `src/views/bills/BillDetailView.vue` | Element Plus → Vant |
| 修改 | `src/views/payment/PaymentView.vue` | Element Plus → Vant |
| 修改 | `src/views/payment/PaymentSuccessView.vue` | Element Plus → Vant |
| 修改 | `src/views/records/RecordsView.vue` | Element Plus → Vant |
| 修改 | `src/views/rooms/RoomsView.vue` | Element Plus → Vant |
| 修改 | `src/views/profile/ProfileView.vue` | Element Plus → Vant |
| 不变 | `src/views/chat/ChatView.vue` | 已原生实现，无需改造 |
| 不变 | `src/api/*` | 后端 API 接口无变化 |
| 不变 | `src/stores/*` | 状态管理无变化 |
| 不变 | `src/utils/request.ts` | 请求拦截器无变化 |

---

## 附录 B：Element Plus → Vant 组件映射表

| Element Plus | Vant 4 | 说明 |
|---|---|---|
| `el-form` + `el-form-item` | `van-form` + `van-field` | 表单，Vant 内置校验 |
| `el-input` | `van-field` | 输入框 |
| `el-button` | `van-button` | 按钮 |
| `el-card` | `van-cell-group` + `van-cell` | 卡片 → 单元格组 |
| `el-tabs` + `el-tab-pane` | `van-tabs` + `van-tab` | 标签页，`sticky` 吸顶 |
| `el-tag` | `van-tag` | 标签 |
| `el-pagination` | `van-pagination` | 分页 |
| `el-empty` | `van-empty` | 空状态 |
| `el-loading`（`v-loading` 指令） | `van-loading`（组件） | 加载中 |
| `el-checkbox` | `van-checkbox` | 复选框 |
| `el-descriptions` | `van-cell-group` | 描述列表 |
| `el-avatar` | `van-image`（round） | 头像 |
| `el-dropdown` | `van-action-sheet` | 下拉菜单 |
| `el-icon` | `van-icon` | 图标 |
| `el-message` | `showNotify` | 消息提示 |
| `ElMessageBox.confirm` | `showConfirmDialog` | 确认对话框 |
| `ElMessageBox.alert` | `showDialog` | 信息对话框 |
| `el-divider` | 无需替代（Vant 单元格自带分隔线） | 分割线 |
| 页面 header | `van-nav-bar` | 顶栏导航 |
| 无 | `van-grid` + `van-grid-item` | 九宫格（Vant 独有，首页用） |
| 无 | `van-tabbar` + `van-tabbar-item` | 底部导航（Vant 独有） |

---

## 附录 C：TabBar 路由设计

```
/login              → 独立页面（无 TabBar，无 NavBar）
/（首页）            → TabBar 页面（Tab 0）
/bills（账单）       → TabBar 页面（Tab 1）
/payment（缴费）     → TabBar 页面（Tab 2）
/profile（我的）     → TabBar 页面（Tab 3）
/bills/:id           → 子页面（NavBar 返回，无 TabBar）
/payment/success/... → 子页面（NavBar 返回，无 TabBar）
/records             → 子页面（NavBar 返回，无 TabBar）
/rooms               → 子页面（NavBar 返回，无 TabBar）
/chat                → 子页面（NavBar 返回，无 TabBar）
```

**设计原则**：TabBar 仅存在于 4 个主页面，子页面使用 NavBar 返回按钮，避免 TabBar 占用屏幕空间同时保持导航清晰。