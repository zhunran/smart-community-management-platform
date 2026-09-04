<template>
  <div class="forum-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>论坛管理</span>
        </div>
      </template>

      <div class="toolbar">
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <el-tab-pane label="待审核" name="0" />
          <el-tab-pane label="已发布" name="1" />
          <el-tab-pane label="已驳回" name="2" />
          <el-tab-pane label="全部" name="" />
        </el-tabs>

        <el-form :inline="true" :model="query" class="search-form">
          <el-form-item label="分类">
            <el-select v-model="query.category" clearable style="width: 140px">
              <el-option v-for="(l, k) in POST_CATEGORY_MAP" :key="k" :label="l" :value="Number(k)" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" clearable placeholder="标题/内容" style="width: 180px" @keyup.enter="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryName || POST_CATEGORY_MAP[row.category] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ POST_STATUS_MAP[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
        <el-table-column prop="likeCount" label="点赞" width="80" align="center" />
        <el-table-column prop="commentCount" label="评论" width="80" align="center" />
        <el-table-column label="置顶" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isPinned === 1"
              :disabled="row.status !== 1"
              @change="(v: boolean) => handleTogglePin(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="加精" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isEssence === 1"
              :disabled="row.status !== 1"
              @change="(v: boolean) => handleToggleEssence(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" @click="openAudit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0" size="small" type="warning" @click="openAudit(row, false)">驳回</el-button>
            <el-button v-if="row.status !== 3" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total,sizes,prev,pager,next,jumper"
          @change="fetchData"
        />
      </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" :title="auditPass ? '通过审核' : '驳回帖子'" width="480px" :close-on-click-modal="false">
      <div class="audit-title">帖子：{{ current?.title }}</div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item v-if="!auditPass" label="驳回原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入驳回原因"
          />
        </el-form-item>
        <el-form-item v-else>
          <div class="audit-tip">确认通过该帖子？通过后将公开展示给业主。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="doAudit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pagePost,
  auditPost,
  togglePin,
  toggleEssence,
  deletePost,
  POST_STATUS_MAP,
  POST_CATEGORY_MAP,
} from '@/api/community'
import type { ForumPostVO } from '@/api/community'

function statusTagType(s: number) {
  const m: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return m[s] || 'info'
}

function fmt(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}

const loading = ref(false)
const tableData = ref<ForumPostVO[]>([])
const total = ref(0)
const activeTab = ref('0')
const query = reactive({
  current: 1,
  size: 20,
  category: undefined as number | undefined,
  keyword: undefined as string | undefined,
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await pagePost({
      current: query.current,
      size: query.size,
      category: query.category,
      keyword: query.keyword,
      status: activeTab.value === '' ? undefined : Number(activeTab.value),
    })
    tableData.value = (res?.data?.records || []).filter(Boolean)
    total.value = Number(res?.data?.total || 0)
  } finally {
    loading.value = false
  }
}
function onTabChange() {
  query.current = 1
  fetchData()
}
function search() {
  query.current = 1
  fetchData()
}
function reset() {
  query.category = undefined
  query.keyword = undefined
  search()
}

function handleTogglePin(row: ForumPostVO, v: boolean) {
  togglePin(row.id)
    .then(() => {
      ElMessage.success(v ? '已置顶' : '已取消置顶')
      fetchData()
    })
    .catch(() => fetchData())
}
function handleToggleEssence(row: ForumPostVO, v: boolean) {
  toggleEssence(row.id)
    .then(() => {
      ElMessage.success(v ? '已加精' : '已取消加精')
      fetchData()
    })
    .catch(() => fetchData())
}

function handleDelete(row: ForumPostVO) {
  ElMessageBox.confirm(`确认删除帖子「${row.title}」？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      await deletePost(row.id)
      ElMessage.success('已删除')
      fetchData()
    })
    .catch(() => {})
}

// ===== 审核 =====
const auditVisible = ref(false)
const auditPass = ref(true)
const auditLoading = ref(false)
const rejectReason = ref('')
const current = ref<ForumPostVO>()

function openAudit(row: ForumPostVO, pass: boolean) {
  current.value = row
  auditPass.value = pass
  rejectReason.value = ''
  auditVisible.value = true
}

async function doAudit() {
  if (!current.value) return
  if (!auditPass.value && !rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  auditLoading.value = true
  try {
    await auditPost(current.value.id, {
      status: auditPass.value ? 1 : 2,
      rejectReason: auditPass.value ? undefined : rejectReason.value,
    })
    ElMessage.success('审核完成')
    auditVisible.value = false
    fetchData()
  } finally {
    auditLoading.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toolbar :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.search-form {
  margin-bottom: 0;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.audit-title {
  color: #303133;
  font-weight: 600;
}
.audit-tip {
  color: #909399;
  line-height: 1.6;
}
</style>
