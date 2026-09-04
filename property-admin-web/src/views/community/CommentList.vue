<template>
  <div class="comment-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>评论管理</span>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="帖子ID" required>
          <el-input v-model="query.postId" clearable placeholder="请输入帖子ID" style="width: 220px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="!searched"
        title="请输入帖子ID后点击查询，查看该帖子下的全部评论"
        type="info"
        :closable="false"
        show-icon
      />

      <template v-else>
        <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
          <el-table-column prop="id" label="评论ID" width="180" align="center" />
          <el-table-column prop="content" label="评论内容" min-width="320" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ COMMENT_STATUS_MAP[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170" align="center">
            <template #default="{ row }">{{ fmt(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 2" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commentPage, deleteComment, COMMENT_STATUS_MAP } from '@/api/community'
import type { ForumCommentVO } from '@/api/community'

function statusTagType(s: number) {
  const m: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return m[s] || 'info'
}

function fmt(s?: string) {
  if (!s) return '-'
  return s.replace('T', ' ').slice(0, 16)
}

const loading = ref(false)
const searched = ref(false)
const tableData = ref<ForumCommentVO[]>([])
const total = ref(0)
const query = reactive({ postId: '', current: 1, size: 20 })

async function fetchData() {
  if (!query.postId) {
    ElMessage.warning('请输入帖子ID')
    return
  }
  loading.value = true
  try {
    const res = await commentPage(query.postId, { current: query.current, size: query.size })
    tableData.value = (res?.data?.records || []).filter(Boolean)
    total.value = Number(res?.data?.total || 0)
    searched.value = true
  } finally {
    loading.value = false
  }
}
function search() {
  query.current = 1
  fetchData()
}

function handleDelete(row: ForumCommentVO) {
  ElMessageBox.confirm('确认删除该评论？删除后业主端将不再展示。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await deleteComment(row.id)
      ElMessage.success('已删除')
      fetchData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 0;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
