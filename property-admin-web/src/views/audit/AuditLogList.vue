<template>
  <div class="audit-page">
    <el-card shadow="never">
      <template #header><div class="card-header"><span>操作审计</span></div></template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="操作者">
          <el-input v-model="query.userName" clearable placeholder="登录账号" style="width:130px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" clearable placeholder="模块名" style="width:130px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总统计 -->
      <el-row :gutter="16" class="summary-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card"><div class="stat-value" style="color:#409eff"><CountUp :end-val="summary.totalCount" /></div><div class="stat-title">区间总操作数</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card"><div class="stat-value" style="color:#e74c3c"><CountUp :end-val="summary.failCount" /></div><div class="stat-title">失败次数</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card"><div class="stat-value" style="color:#e6a23c"><CountUp :end-val="summary.failRate" suffix="%" /></div><div class="stat-title">失败率</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card"><div class="stat-value" style="color:#764ba2"><CountUp :end-val="riskTotal" /></div><div class="stat-title">风险动作次数</div></el-card>
        </el-col>
      </el-row>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column prop="userName" label="账号" width="110" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="module" label="模块" width="110" show-overflow-tooltip />
        <el-table-column prop="action" label="动作" width="130" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80" align="center" />
        <el-table-column prop="requestUrl" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="right" />
        <el-table-column prop="resultMsg" label="结果" min-width="140" show-overflow-tooltip />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total,sizes,prev,pager,next,jumper"
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs, getAuditSummary, exportAuditLogs } from '@/api/statistic'
import type { AuditLogItem, AuditLogQuery, AuditSummary } from '@/api/statistic'
import CountUp from '@/components/CountUp.vue'

const loading = ref(false)
const tableData = ref<AuditLogItem[]>([])
const total = ref(0)
const range = ref<[string, string] | null>(null)

const query = reactive<AuditLogQuery>({
  pageNum: 1,
  pageSize: 20,
  status: undefined,
  module: '',
  userName: '',
})

const summary = ref<AuditSummary>({ totalCount: 0, failCount: 0, failRate: 0, moduleTop: [], userTop: [], riskActionCount: {} })
const riskTotal = computed(() =>
  Object.values(summary.value.riskActionCount || {}).reduce((s, v) => s + (Number(v) || 0), 0),
)

function buildParams(withPage: boolean): AuditLogQuery {
  const params: AuditLogQuery = { module: query.module || undefined, userName: query.userName || undefined, status: query.status }
  if (range.value) {
    params.start = range.value[0]
    params.end = range.value[1]
  }
  if (withPage) {
    params.pageNum = query.pageNum
    params.pageSize = query.pageSize
  }
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAuditLogs(buildParams(true))
    tableData.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const res = await getAuditSummary(buildParams(false))
    summary.value = res.data
  } catch {
    /* 汇总失败不影响列表 */
  }
}

function search() {
  query.pageNum = 1
  fetchData()
  fetchSummary()
}

function reset() {
  query.status = undefined
  query.module = ''
  query.userName = ''
  range.value = null
  search()
}

async function handleExport() {
  try {
    const res = await exportAuditLogs(buildParams(false))
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `操作审计_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  fetchData()
  fetchSummary()
})
</script>

<style scoped>
.audit-page { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
.summary-row { margin: 4px 0 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 26px; font-weight: bold; line-height: 1.4; }
.stat-title { font-size: 13px; color: #909399; margin-top: 6px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>