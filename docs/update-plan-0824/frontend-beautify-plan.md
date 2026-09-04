# 管理端前端美化实施计划

> 创建日期：2026-08-24
> 范围：property-admin-web 管理端登录页 + 内部布局 + 通用体验优化
> 原则：只加效果，不新增功能，不改业务逻辑

---

## 实施顺序

```
Phase 1（低难度，快速见效）→ Phase 2（中难度，核心亮点）→ Phase 3（锦上添花）
```

---

## Phase 1：低难度快速改造

### 1.1 NProgress 顶部进度条

- **改动文件**：`src\router\index.ts`
- **新增依赖**：`nprogress` + `@types/nprogress`
- **代码量**：~10 行
- **实现方式**：
  - 路由守卫 `beforeEach` 中调用 `NProgress.start()`
  - `afterEach` 中调用 `NProgress.done()`
  - 在 `src\App.vue` 引入 `nprogress/nprogress.css`，自定义颜色为品牌色

### 1.2 页面切换过渡动画

- **改动文件**：`src\layouts\MainLayout.vue`
- **代码量**：~5 行
- **实现方式**：
  - `<router-view>` 外层包裹 `<transition>` 组件
  - 使用 `mode="out-in"` 实现先出后入
  - 定义 `fade-slide` 过渡类（opacity + translateY）

### 1.3 侧边栏折叠

- **改动文件**：`src\layouts\MainLayout.vue`
- **代码量**：~30 行
- **实现方式**：
  - 添加 `isCollapse` 响应式变量
  - `el-aside` 宽度动态绑定 `:width="isCollapse ? '64px' : '220px'"`
  - `el-menu` 添加 `:collapse="isCollapse"` 属性
  - 顶部 Header 左侧添加折叠按钮（`Fold` / `Expand` 图标切换）
  - 侧边栏添加 CSS transition 过渡动画

---

## Phase 2：中难度核心美化

### 2.1 登录页全面美化

- **改动文件**：`src\views\login\LoginView.vue`
- **代码量**：纯 CSS ~80 行（无新增依赖）
- **改造内容**：

| 区域 | 当前 | 优化后 |
|------|------|--------|
| 背景 | 静态紫色渐变 `#667eea → #764ba2` | 多层径向渐变 + `@keyframes` 流动动画 |
| 卡片 | 纯白背景 + 阴影 | 玻璃态：`background: rgba(255,255,255,0.85)` + `backdrop-filter: blur(20px)` |
| 标题 | 纯文字 | 加系统图标（`OfficeBuilding`），文字渐变着色 |
| 输入框 | 默认样式 | hover/focus 时边框发光（`box-shadow: 0 0 0 2px rgba(102,126,234,0.2)`） |
| 登录按钮 | 纯色蓝 | 渐变背景 + hover 时 `scale(1.02)` 微缩放 + 阴影扩散 |
| 卡片入场 | 无 | 从 `translateY(30px)` + `opacity(0)` 淡入上浮 |

- **背景动画原理**：两层伪元素 `::before` / `::after` 各画一个径向渐变圆，以不同速度做圆周运动，产生流动光影效果

### 2.2 仪表盘数字滚动动画

- **改动文件**：`src\views\dashboard\DashboardView.vue`
- **新增依赖**：`countup.js`（或纯手写 ~30 行）
- **代码量**：~40 行
- **实现方式**：
  - 封装 `<CountUp :end-val="num" />` 组件
  - 使用 `requestAnimationFrame` + `easeOutExpo` 缓动函数
  - 数字从 0 滚动到目标值，持续 1.5s
  - 统计卡片进入视口时触发（`IntersectionObserver`）

---

## Phase 3：内部布局细节优化

### 3.1 侧边栏样式升级

- **改动文件**：`src\layouts\MainLayout.vue`
- **代码量**：~30 行
- **改造内容**：

| 区域 | 当前 | 优化后 |
|------|------|--------|
| 背景 | 纯色 `#304156` | 深色渐变 `linear-gradient(180deg, #1e2a3a 0%, #2d4059 100%)` |
| Logo | 纯文字"物业管理系统" | 文字 + 图标（`OfficeBuilding`），字体加粗带字间距 |
| 菜单项 | 无过渡 | hover 时背景色平滑过渡 + 左侧 3px 指示条 |
| 激活项 | 纯色高亮 | 左侧圆角标记 + 背景渐变高亮 |
| 底部 | 无 | 折叠按钮 + 版本号小字 |

### 3.2 顶部栏样式升级

- **改动文件**：`src\layouts\MainLayout.vue`
- **代码量**：~15 行
- **改造内容**：

| 区域 | 当前 | 优化后 |
|------|------|--------|
| 背景 | 纯白 | 白色 + 底部微阴影 `box-shadow: 0 1px 4px rgba(0,0,0,0.08)` |
| 左侧 | 无 | 面包屑导航（基于 `route.meta.title`） |
| 右侧 | 纯文字用户名 | 用户头像圆 + 用户名 + 下拉箭头 |
| 整体 | 无 | 添加 `z-index` 确保在内容之上 |

### 3.3 内容区视觉层次

- **改动文件**：`src\layouts\MainLayout.vue` + 各页面组件
- **代码量**：~20 行
- **改造内容**：
  - `el-main` 背景改为 `#f5f7fa`（更柔和）
  - 各页面内容区统一包裹白色圆角卡片（`background: #fff; border-radius: 8px; padding: 20px;`）
  - 页面标题统一用 `<h3>` 加左侧色条装饰

---

## 骨架屏

- **改动文件**：各页面组件（DashboardView、BillList、OwnerList 等）
- **代码量**：每个页面 ~15 行
- **实现方式**：
  - 用 `el-skeleton` 组件包裹数据区域
  - `v-loading` 改为 `:loading` 控制骨架屏显隐
  - 数据加载完成后切换为真实内容
  - 建议优先对仪表盘和列表页添加

---

## 文件改动总览

| 文件 | Phase | 改动内容 |
|------|-------|----------|
| `src\router\index.ts` | 1 | NProgress 进度条 |
| `src\App.vue` | 1 | NProgress 样式引入 |
| `src\layouts\MainLayout.vue` | 1, 3 | 页面过渡、侧边栏折叠、侧边栏/顶部栏/内容区样式 |
| `src\views\login\LoginView.vue` | 2 | 登录页全面美化 |
| `src\views\dashboard\DashboardView.vue` | 2 | 数字滚动动画、骨架屏 |
| 各列表页面 | 3 | 骨架屏、卡片包裹 |

---

## 预估工时

| Phase | 内容 | 预估 |
|-------|------|------|
| Phase 1 | NProgress + 页面过渡 + 侧边栏折叠 | 30 分钟 |
| Phase 2 | 登录页美化 + 数字滚动 | 1 小时 |
| Phase 3 | 侧边栏/顶部栏/内容区 + 骨架屏 | 1 小时 |
| **合计** | | **~2.5 小时** |

---

## 依赖清单

```json
{
  "nprogress": "^0.2.0",
  "@types/nprogress": "^0.2.0"
}
```

> `countup.js` 可选，数字滚动可手写无需额外依赖。