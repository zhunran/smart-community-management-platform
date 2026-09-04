<template>
  <div class="notice-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <div><el-button type="primary" @click="handleAdd">新建公告</el-button></div>
        </div>
      </template>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="类型">
          <el-select v-model="query.type" clearable style="width:140px">
            <el-option v-for="(l, k) in TYPE_MAP" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width:110px">
            <el-option v-for="(l, k) in STATUS_MAP" :key="k" :label="l" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">{{ TYPE_MAP[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ STATUS_MAP[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160" align="center">
          <template #default="{ row }">{{ row.publishTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" v-if="row.status === 0" @click="handlePublish(row)">发布</el-button>
            <el-button size="small" type="warning" v-if="row.status === 1" @click="handleOffline(row)">下线</el-button>
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
    <!-- 新建公告 -->
    <el-dialog v-model="dialogVisible" title="新建公告" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="(l, k) in TYPE_MAP" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageNotice, createNotice, publishNotice, offlineNotice, NOTICE_TYPE_MAP, NOTICE_STATUS_MAP } from '@/api/notice'
import type { NoticeVO, NoticeCreateRequest } from '@/api/notice'

const TYPE_MAP = NOTICE_TYPE_MAP
const STATUS_MAP = NOTICE_STATUS_MAP

function typeTagType(t: string) {
  const m: Record<string, string> = { NOTICE: 'info', WATER_ELECTRIC: 'warning', ACTIVITY: 'success', EMERGENCY: 'danger' }
  return m[t] || 'info'
}
function statusTagType(s: number) {
  return s === 0 ? 'info' : s === 1 ? 'success' : 'warning'
}

const loading = ref(false)
const tableData = ref<NoticeVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, type: undefined as string | undefined, status: undefined as number | undefined })

onMounted(() => { fetchData() })

async function fetchData() {
  loading.value = true
  try {
    const res = await pageNotice(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally { loading.value = false }
}
function search() { query.current = 1; fetchData() }
function reset() { query.type = undefined; query.status = undefined; search() }

// create
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ title: '', content: '', type: 'NOTICE' })
const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}
function handleAdd() { form.title = ''; form.content = ''; form.type = 'NOTICE'; dialogVisible.value = true }
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const data: NoticeCreateRequest = { title: form.title, content: form.content, type: form.type }
    await createNotice(data)
    ElMessage.success('公告创建成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitLoading.value = false }
}

// publish
function handlePublish(row: NoticeVO) {
  ElMessageBox.confirm(`确认发布公告「${row.title}」？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'success' })
    .then(async () => { await publishNotice(row.id); ElMessage.success('已发布'); fetchData() })
    .catch(() => {})
}

// offline
function handleOffline(row: NoticeVO) {
  ElMessageBox.confirm(`确认下线公告「${row.title}」？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => { await offlineNotice(row.id); ElMessage.success('已下线'); fetchData() })
    .catch(() => {})
}
</script>
<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px }
.search-form { margin-bottom: 0 }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end }
</style>