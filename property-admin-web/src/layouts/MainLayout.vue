<template>
  <div class="layout">
    <el-container style="height: 100vh">
      <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
        <div class="logo">
          <el-icon :size="22"><OfficeBuilding /></el-icon>
          <span v-show="!isCollapse" class="logo-text">物业管理系统</span>
        </div>
        <el-menu
          :default-active="route.path"
          router
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="transparent"
          text-color="rgba(255,255,255,0.7)"
          active-text-color="#fff"
          class="sidebar-menu"
        >
          <el-menu-item index="/">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-sub-menu index="1">
            <template #title
              ><el-icon><HomeFilled /></el-icon><span>小区管理</span></template
            >
            <el-menu-item index="/building">楼栋管理</el-menu-item>
            <el-menu-item index="/unit">单元管理</el-menu-item>
            <el-menu-item index="/room">房屋管理</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/owner">
            <el-icon><UserFilled /></el-icon>
            <span>业主管理</span>
          </el-menu-item>
          <el-menu-item index="/notice">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/sys-config">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
          <el-sub-menu index="2">
            <template #title
              ><el-icon><Money /></el-icon><span>收费管理</span></template
            >
            <el-menu-item index="/fee-item">费用项管理</el-menu-item>
            <el-menu-item index="/fee-standard">费用标准管理</el-menu-item>
            <el-menu-item index="/bill">账单管理</el-menu-item>
            <el-menu-item index="/payment">缴费记录</el-menu-item>
            <el-menu-item index="/report">收费报表</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="4">
            <template #title
              ><el-icon><TrendCharts /></el-icon><span>报表统计</span></template
            >
            <el-menu-item index="/report/trend">缴费趋势</el-menu-item>
            <el-menu-item index="/audit">操作审计</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="3">
            <template #title
              ><el-icon><Coin /></el-icon><span>车位管理</span></template
            >
            <el-menu-item index="/parking">车位维护</el-menu-item>
            <el-menu-item index="/parking-warning">对账预警</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="5">
            <template #title
              ><el-icon><ChatDotRound /></el-icon
              ><span>社区管理</span></template
            >
            <el-menu-item index="/community/activity">活动管理</el-menu-item>
            <el-menu-item index="/community/forum">论坛管理</el-menu-item>
            <el-menu-item index="/community/comment">评论管理</el-menu-item>
            <el-menu-item index="/community/vote">投票管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="6">
            <template #title
              ><el-icon><Service /></el-icon><span>服务管理</span></template
            >
            <el-menu-item index="/service/repair">报修工单</el-menu-item>
            <el-menu-item index="/service/venue">场地管理</el-menu-item>
            <el-menu-item index="/service/visitor">访客记录</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.meta.title">{{
                route.meta.title
              }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="28" class="user-avatar">{{
                  (userStore.realName || userStore.username).charAt(0)
                }}</el-avatar>
                <span class="user-name">{{
                  userStore.realName || userStore.username
                }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="main">
          <router-view v-slot="{ Component }">
            <transition name="page-fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import {
  DataAnalysis,
  ArrowDown,
  HomeFilled,
  UserFilled,
  Money,
  Coin,
  Bell,
  Fold,
  Expand,
  OfficeBuilding,
  Setting,
  ChatDotRound,
  Service,
} from "@element-plus/icons-vue";
import { ElMessageBox } from "element-plus";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapse = ref(false);

function handleCommand(command: string) {
  if (command === "logout") {
    ElMessageBox.confirm("确认退出登录？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    })
      .then(async () => {
        await userStore.logout();
        router.push("/login");
      })
      .catch(() => {});
  }
}
</script>

<style scoped>
/* ===== 侧边栏 ===== */
.sidebar {
  background: linear-gradient(180deg, #1e2a3a 0%, #2d4059 100%);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s ease;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  letter-spacing: 2px;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  transition: background-color 0.3s ease;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(
    90deg,
    rgba(64, 158, 255, 0.3) 0%,
    rgba(64, 158, 255, 0.1) 100%
  );
  border-left: 3px solid #409eff;
}

/* ===== 顶部栏 ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  color: #606266;
  cursor: pointer;
  transition: color 0.3s;
}
.collapse-btn:hover {
  color: #409eff;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
}

.user-avatar {
  background: linear-gradient(135deg, #409eff, #764ba2);
  color: #fff;
  font-size: 14px;
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 内容区 ===== */
.main {
  background-color: #f5f7fa;
  padding: 20px;
  min-height: 0;
}

/* ===== 页面切换过渡 ===== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
