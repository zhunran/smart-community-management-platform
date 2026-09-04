<template>
  <div class="activity-detail">
    <van-nav-bar title="活动详情" left-text="返回" left-arrow @click-left="router.back()" />

    <div v-if="loading" class="loading-box">
      <van-skeleton title :row="6" />
    </div>

    <template v-else-if="detail">
      <div class="cover">
        <van-image v-if="detail.coverImage" :src="detail.coverImage" fit="cover" class="cover-img" />
        <div v-else class="cover-placeholder">
          <van-icon name="smile-o" size="40" color="#cbd5e1" />
        </div>
      </div>

      <div class="body">
        <div class="title-row">
          <span class="title">{{ detail.title }}</span>
          <van-tag :color="statusColor(detail.status)" text-color="#fff" round>{{ detail.statusName }}</van-tag>
        </div>

        <div class="meta-list">
          <div class="meta-item">
            <van-icon name="label-o" />
            <span>{{ detail.activityTypeName }}</span>
          </div>
          <div class="meta-item">
            <van-icon name="clock-o" />
            <span>{{ fmt(detail?.startTime) }} ~ {{ fmt(detail?.endTime) }}</span>
          </div>
          <div class="meta-item">
            <van-icon name="location-o" />
            <span>{{ detail.location }}</span>
          </div>
          <div class="meta-item">
            <van-icon name="manager-o" />
            <span>组织者：{{ detail.organizer }}</span>
          </div>
        </div>

        <div class="progress-box">
          <div class="progress-label">
            <span>报名人数</span>
            <span>{{ detail.signupCount }} / {{ detail.maxParticipants }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress-inner" :style="{ width: progressPercent + '%' }" />
          </div>
        </div>

        <div class="section">
          <div class="section-title">活动详情</div>
          <div class="content">{{ detail.content }}</div>
        </div>
      </div>

      <div class="footer-bar">
        <van-button
          v-if="detail.status === 1 && !detail.isSignedUp"
          type="primary"
          block
          round
          :loading="actionLoading"
          @click="handleSignup"
        >
          立即报名
        </van-button>
        <van-button
          v-else-if="detail.status === 1 && detail.isSignedUp"
          block
          round
          color="#f59e0b"
          :loading="actionLoading"
          @click="handleCancelSignup"
        >
          取消报名
        </van-button>
        <van-button v-else block round disabled>{{ btnText }}</van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showConfirmDialog, showFailToast } from 'vant'
import { getActivity, signupActivity, cancelSignupActivity } from '@/api/community'
import type { CommunityActivityDetailVO } from '@/api/community'

const route = useRoute()
const router = useRouter()
const id = String(route.params.id)

function fmt(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}
function statusColor(s: number) {
  const m: Record<number, string> = { 1: '#3b82f6', 2: '#f59e0b', 3: '#22c55e', 4: '#94a3b8', 5: '#ef4444' }
  return m[s] || '#94a3b8'
}

const loading = ref(false)
const detail = ref<CommunityActivityDetailVO>()
const actionLoading = ref(false)

const progressPercent = computed(() => {
  if (!detail.value?.maxParticipants) return 0
  return Math.min(100, Math.round((detail.value.signupCount / detail.value.maxParticipants) * 100))
})

const btnText = computed(() => {
  const s = detail.value?.status
  const m: Record<number, string> = { 0: '未开始报名', 2: '已满员', 3: '活动进行中', 4: '活动已结束', 5: '活动已取消' }
  return m[s ?? 0] || '暂不可报名'
})

async function load() {
  loading.value = true
  try {
    const res = await getActivity(id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleSignup() {
  actionLoading.value = true
  try {
    await signupActivity(id)
    showSuccessToast('报名成功')
    await load()
  } catch {
    showFailToast('报名失败，可能名额已满，请重试')
  } finally {
    actionLoading.value = false
  }
}

function handleCancelSignup() {
  showConfirmDialog({ title: '提示', message: '确认取消报名？' })
    .then(async () => {
      actionLoading.value = true
      try {
        await cancelSignupActivity(id)
        showSuccessToast('已取消报名')
        await load()
      } finally {
        actionLoading.value = false
      }
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.activity-detail {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 70px;
}
.loading-box {
  padding: 20px;
}
.cover {
  height: 200px;
  background: #eef2f7;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #f5f3ff 100%);
}
.body {
  padding: 16px;
}
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}
.title {
  font-size: 19px;
  font-weight: 600;
  color: #1e293b;
}
.meta-list {
  background: #fff;
  border-radius: 12px;
  padding: 6px 14px;
  margin-bottom: 14px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 0;
  font-size: 14px;
  color: #475569;
}
.meta-item .van-icon {
  color: #3b82f6;
}
.progress-box {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 14px;
}
.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}
.progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}
.progress-inner {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 4px;
  transition: width 0.4s ease;
}
.section {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 10px;
}
.content {
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.06);
}
</style>
