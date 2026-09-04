<template>
  <div class="page">
    <van-nav-bar title="社区公告" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="notice in notices"
          :key="notice.id"
          :title="notice.title"
          is-link
          :label="noticeType(notice.type) + ' · ' + formatTime(notice.publishTime)"
          @click="showDetail(notice)"
        >
          <template #icon v-if="isEmergency(notice.type)">
            <van-icon name="warning" class="notice-icon" />
          </template>
        </van-cell>
        <van-empty v-if="notices.length === 0" description="暂无公告" />
      </template>
    </div>

    <!-- 公告详情 -->
    <van-dialog v-model:show="detailVisible" :title="current?.title" :show-confirm-button="true" confirm-button-text="知道了">
      <div class="notice-content">{{ current?.content }}</div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getNotices, NOTICE_TYPE_MAP } from '@/api/notice'
import type { NoticeVO } from '@/api/notice'

const router = useRouter()
const loading = ref(false)
const notices = ref<NoticeVO[]>([])
const detailVisible = ref(false)
const current = ref<NoticeVO>()

function goBack() { router.back() }
function noticeType(t: string) { return NOTICE_TYPE_MAP[t] || '公告' }
function isEmergency(t: string) { return t === 'EMERGENCY' }
function formatTime(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}
function showDetail(n: NoticeVO) {
  current.value = n
  detailVisible.value = true
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getNotices()
    notices.value = Array.isArray(res.data) ? res.data : []
    if (res.data && !Array.isArray(res.data)) showToast('数据异常')
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
.notice-icon {
  color: #ee0a24;
  font-size: 20px;
  margin-right: 8px;
}
.notice-content {
  padding: 8px 20px 20px;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
  font-size: 14px;
  text-align: left;
}
</style>