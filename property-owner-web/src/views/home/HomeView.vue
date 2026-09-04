<template>
  <div class="home">
    <!-- 顶部栏 -->
    <div class="home-header">
      <div class="header-left">
        <div class="header-logo">
          <van-icon name="home-o" size="20" color="#3b82f6" />
        </div>
        <span class="header-title">智慧物业</span>
      </div>
      <div class="header-right" @click="showMenu = true">
        <span class="user-name">{{ ownerStore.ownerName }}</span>
        <van-icon name="arrow-down" size="12" color="#94a3b8" />
      </div>
    </div>

    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <div class="welcome-avatar">
        <span class="avatar-text">{{ ownerStore.ownerName?.charAt(0) || '业' }}</span>
      </div>
      <div class="welcome-info">
        <p class="welcome-greeting">下午好</p>
        <p class="welcome-name">{{ ownerStore.ownerName }}</p>
      </div>
      <div class="welcome-extra">
        <span class="welcome-phone">{{ maskPhone(ownerStore.phone) }}</span>
      </div>
    </div>

    <!-- 快捷功能区 -->
    <div class="section-title">常用服务</div>
    <div class="service-grid">
      <div class="service-item" v-for="item in services" :key="item.path" @click="router.push(item.path)">
        <div class="service-icon" :style="{ background: item.bg }">
          <van-icon :name="item.icon" size="22" :color="item.color" />
        </div>
        <span class="service-name">{{ item.name }}</span>
      </div>
    </div>

    <!-- 用户菜单 -->
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

const services = [
  { name: '我的账单', icon: 'orders-o', path: '/bills', bg: '#eff6ff', color: '#3b82f6' },
  { name: '在线缴费', icon: 'balance-o', path: '/payment', bg: '#fef3c7', color: '#f59e0b' },
  { name: '支付记录', icon: 'records-o', path: '/records', bg: '#f0fdf4', color: '#22c55e' },
  { name: '我的房屋', icon: 'home-o', path: '/rooms', bg: '#fdf2f8', color: '#ec4899' },
  { name: '社区公告', icon: 'bell', path: '/notices', bg: '#fef2f2', color: '#ef4444' },
  { name: '个人信息', icon: 'user-o', path: '/profile', bg: '#f5f3ff', color: '#8b5cf6' },
  { name: 'AI 客服', icon: 'service-o', path: '/chat', bg: '#ecfeff', color: '#06b6d4' },
]

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

/* 顶部栏 */
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-logo {
  width: 32px;
  height: 32px;
  background: #eff6ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 16px;
  background: #f1f5f9;
  transition: background 0.2s;
}

.header-right:active {
  background: #e2e8f0;
}

.user-name {
  font-size: 13px;
  color: #475569;
}

/* 欢迎卡片 */
.welcome-card {
  display: flex;
  align-items: center;
  margin: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border-radius: 14px;
  color: #fff;
  gap: 14px;
}

.welcome-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  font-size: 20px;
  font-weight: 600;
}

.welcome-info {
  flex: 1;
  min-width: 0;
}

.welcome-greeting {
  font-size: 13px;
  opacity: 0.8;
  margin-bottom: 2px;
}

.welcome-name {
  font-size: 18px;
  font-weight: 600;
}

.welcome-extra {
  flex-shrink: 0;
}

.welcome-phone {
  font-size: 12px;
  opacity: 0.7;
  background: rgba(255, 255, 255, 0.15);
  padding: 2px 10px;
  border-radius: 10px;
}

/* 分区标题 */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  padding: 0 16px;
  margin-bottom: 12px;
}

/* 服务网格 */
.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 0 16px 20px;
}

.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.service-item:active {
  transform: scale(0.96);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.service-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.service-name {
  font-size: 12px;
  color: #475569;
}
</style>