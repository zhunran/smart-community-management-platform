<template>
  <div class="dashboard">
    <el-card class="filter-card" style="margin-bottom: 20px">
      <el-date-picker
        v-model="period"
        type="month"
        value-format="YYYY-MM"
        placeholder="选择统计账期"
        :clearable="false"
        @change="loadData"
      />
      <span class="filter-tip">统计账期{{ period }}</span>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="6" v-for="card in overviewCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">
            <CountUp :end-val="card.rawValue" :prefix="card.prefix" :suffix="card.suffix" />
          </div>
          <div class="stat-title">{{ card.title }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" style="margin-top:20px">
      <template #header><span>各费用项收缴率</span></template>
      <el-table :data="feeStats" v-loading="feeLoading" stripe border size="small" style="width:100%"
                empty-text="该账期暂无账单数据">
        <el-table-column prop="feeItemName" label="费用项" width="140" />
        <el-table-column prop="receivable" label="应收" width="120" align="right"><template #default="{row}">¥{{ row.receivable }}</template></el-table-column>
        <el-table-column prop="received" label="实收" width="120" align="right"><template #default="{row}">¥{{ row.received }}</template></el-table-column>
        <el-table-column prop="billCount" label="户数" width="70" align="center" />
        <el-table-column prop="paidCount" label="已缴" width="70" align="center" />
        <el-table-column prop="collectionRate" label="收缴率" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="row.collectionRate>=80?'success':row.collectionRate>=50?'warning':'danger'" size="small">{{ row.collectionRate }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card" style="margin-top:20px">
      <template #header><span>欢迎使用物业管理收费系统</span></template>
      <p>当前登录：{{ userStore.realName || userStore.username }} | 统计月份：{{ overview.statisticsMonth }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDashboardOverview, getFeeItemStats } from '@/api/report'
import type { DashboardVO, FeeItemStatVO } from '@/api/report'
import CountUp from '@/components/CountUp.vue'

const userStore = useUserStore()

// 默认统计当前月，可切换账期查看历史月份
const now = new Date()
const period = ref<string>(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)

const overview = ref<DashboardVO>({ currentMonthReceivable: 0, currentMonthReceived: 0, totalArrears: 0, collectionRate: 0, statisticsMonth: '' })
const overviewCards = ref<{ title: string; prefix: string; suffix: string; rawValue: number; color: string }[]>([])
const feeStats = ref<FeeItemStatVO[]>([])
const feeLoading = ref(false)

async function loadData() {
  try {
    const overviewRes = await getDashboardOverview(period.value)
    overview.value = overviewRes.data as DashboardVO
    overviewCards.value = [
      { title: `当月应收 (${overview.value.statisticsMonth})`, prefix: '¥', suffix: '', rawValue: overview.value.currentMonthReceivable, color: '#409eff' },
      { title: `当月实收 (${overview.value.statisticsMonth})`, prefix: '¥', suffix: '', rawValue: overview.value.currentMonthReceived, color: '#67c23a' },
      { title: '累计欠费', prefix: '¥', suffix: '', rawValue: overview.value.totalArrears, color: '#e74c3c' },
      { title: '当月收缴率', prefix: '', suffix: '%', rawValue: overview.value.collectionRate, color: overview.value.collectionRate >= 80 ? '#67c23a' : '#e6a23c' },
    ]
  } catch {}
  feeLoading.value = true
  try {
    const res = await getFeeItemStats(period.value)
    feeStats.value = res.data as FeeItemStatVO[]
  } finally { feeLoading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.filter-card :deep(.el-card__body) { display: flex; align-items: center; gap: 12px; }
.filter-tip { font-size: 13px; color: #909399; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-title { font-size: 14px; color: #909399; margin-top: 8px; }
.section-card :deep(.el-card__header) { font-weight: bold; }
</style>