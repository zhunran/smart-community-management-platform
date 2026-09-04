<template>
  <div class="report-list">
    <el-card>
      <template #header><div class="card-header"><span>收费报表</span></div></template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="账期">
          <el-input v-model="period" placeholder="如 2026-06" style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleExport">导出月度报表</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 style="margin-bottom:12px">按费用项统计</h3>
      <el-form :inline="true">
        <el-form-item label="月份">
          <el-input v-model="statsPeriod" placeholder="如 2026-06" style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchStats">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="statsData" v-loading="statsLoading" stripe border style="width:100%">
        <el-table-column prop="feeItemName" label="费用项" width="140" />
        <el-table-column prop="receivable" label="应收金额" width="120" align="right"><template #default="{row}">¥{{ row.receivable }}</template></el-table-column>
        <el-table-column prop="received" label="实收金额" width="120" align="right"><template #default="{row}">¥{{ row.received }}</template></el-table-column>
        <el-table-column prop="billCount" label="应收户数" width="90" align="center" />
        <el-table-column prop="paidCount" label="已缴户数" width="90" align="center" />
        <el-table-column prop="collectionRate" label="收缴率" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="row.collectionRate>=80?'success':row.collectionRate>=50?'warning':'danger'" size="small">
              {{ row.collectionRate }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 概览卡片 -->
      <el-row :gutter="20" style="margin-top:24px">
        <el-col :span="6" v-for="card in overviewCards" :key="card.title">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" :style="{color:card.color}">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="overviewStats" v-loading="overviewLoading" stripe border style="width:100%;margin-top:16px">
        <el-table-column prop="feeItemName" label="费用项" width="140" />
        <el-table-column prop="receivable" label="应收" width="120" align="right"><template #default="{row}">¥{{ row.receivable }}</template></el-table-column>
        <el-table-column prop="received" label="实收" width="120" align="right"><template #default="{row}">¥{{ row.received }}</template></el-table-column>
        <el-table-column prop="collectionRate" label="收缴率" width="100" align="center">
          <template #default="{row}"><el-tag :type="row.collectionRate>=80?'success':'warning'" size="small">{{ row.collectionRate }}%</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportMonthlyReport, getDashboardOverview, getFeeItemStats } from '@/api/report'
import type { DashboardVO, FeeItemStatVO } from '@/api/report'

const period = ref('')
const statsPeriod = ref('')

// 报表导出
async function handleExport() {
  if (!period.value) { ElMessage.warning('请输入账期'); return }
  try {
    const res = await exportMonthlyReport(period.value)
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `收费报表_${period.value}.xlsx`; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') }
}

// 费用项统计
const statsLoading = ref(false)
const statsData = ref<FeeItemStatVO[]>([])
async function fetchStats() {
  statsLoading.value = true
  try {
    const res = await getFeeItemStats(statsPeriod.value || undefined)
    statsData.value = res.data
  } finally { statsLoading.value = false }
}

// 概览
const overviewLoading = ref(false)
const overviewStats = ref<FeeItemStatVO[]>([])
const overviewCards = ref<{ title: string; value: string; color: string }[]>([])

onMounted(async () => {
  // 默认加载当前月
  const now = new Date()
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  period.value = month
  statsPeriod.value = month

  // 加载概览
  overviewLoading.value = true
  try {
    const [overviewRes, statsRes] = await Promise.all([
      getDashboardOverview(),
      getFeeItemStats(),
    ])
    const overview = overviewRes.data as DashboardVO
    const stats = statsRes.data as FeeItemStatVO[]
    overviewStats.value = stats

    overviewCards.value = [
      { title: `当月应收 (${overview.statisticsMonth})`, value: `¥${overview.currentMonthReceivable}`, color: '#409eff' },
      { title: `当月实收 (${overview.statisticsMonth})`, value: `¥${overview.currentMonthReceived}`, color: '#67c23a' },
      { title: '累计欠费', value: `¥${overview.totalArrears}`, color: '#e74c3c' },
      { title: '当月收缴率', value: `${overview.collectionRate}%`, color: overview.collectionRate >= 80 ? '#67c23a' : '#e6a23c' },
    ]
  } finally { overviewLoading.value = false }

  // 加载费用项统计
  await fetchStats()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-title { font-size: 13px; color: #909399; margin-top: 6px; }
</style>
