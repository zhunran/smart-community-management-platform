<template>
  <div class="tab-layout">
    <div class="tab-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
    <van-tabbar
      v-model="activeTab"
      :fixed="true"
      :border="false"
      active-color="#3b82f6"
      inactive-color="#94a3b8"
      :safe-area-inset-bottom="true"
    >
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/community">社区</van-tabbar-item>
      <van-tabbar-item icon="apps-o" to="/service">服务</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/bills">账单</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activeTab = ref(0);

const tabMap: Record<string, number> = {
  "/": 0,
  "/community": 1,
  "/service": 2,
  "/bills": 3,
  "/profile": 4,
};

watch(
  () => route.path,
  (path) => {
    if (tabMap[path] !== undefined) {
      activeTab.value = tabMap[path];
    }
  },
  { immediate: true },
);
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
  padding-bottom: 50px;
}

/* 页面切换过渡 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* TabBar 顶部阴影 */
:deep(.van-tabbar) {
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.04);
}
</style>
