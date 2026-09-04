<template>
  <div class="fee-trend-page">
    <div class="page-toolbar">
      <el-card shadow="never">
        <el-form inline class="toolbar-form">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="range"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :clearable="false"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetQuick('7')">近7天</el-button>
            <el-button @click="resetQuick('30')">近30天</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-card shadow="never" class="chart-card">
      <div class="chart-summary" v-if="summary.amount > 0">
        <div class="summary-item">
          <span class="label">区间缴费人数</span>
          <CountUp :end-val="summary.payerCount" class="value" suffix=" 人" />
        </div>
        <div class="summary-item">
          <span class="label">区间缴费金额</span>
          <CountUp :end-val="summary.amount" class="value money" prefix="¥ " />
        </div>
      </div>
      <div class="chart-box" v-show="data.length">
        <div ref="chartRef" class="chart" v-loading="loading" />
      </div>
      <el-empty v-if="!loading && !data.length" description="暂无可展示的缴费趋势数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { LegacyGridContainLabel } from 'echarts/features'
import type { ECharts } from 'echarts/core'
import { getFeeTrend } from '@/api/statistic'
import type { FeeTrendPoint } from '@/api/statistic'
import CountUp from '@/components/CountUp.vue'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer, LegacyGridContainLabel])

function fmtDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const range = ref<[string, string]>([fmtDate(new Date(Date.now() - 29 * 86400000)), fmtDate(new Date())])
const data = ref<FeeTrendPoint[]>([])
const loading = ref(false)
const chartRef = ref<HTMLElement>()
let chart: ECharts | undefined

const summary = reactive({ payerCount: 0, amount: 0 })

async function fetchData() {
  loading.value = true
  try {
    const res = await getFeeTrend(range.value[0], range.value[1])
    data.value = res.data || []
    const payer = data.value.reduce((s, p) => s + (Number(p.payerCount) || 0), 0)
    const amount = data.value.reduce((s, p) => s + (Number(p.amount) || 0), 0)
    summary.payerCount = payer
    summary.amount = amount
    // v-show 由 data.length 驱动：等待 DOM patch 完成容器可见后再初始化图表，
    // 避免 echarts.init 拿到 display:none 的 0 尺寸容器导致点线挤在 Y 轴
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function resetQuick(days: string) {
  const end = new Date()
  const start = new Date(Date.now() - (Number(days) - 1) * 86400000)
  range.value = [fmtDate(start), fmtDate(end)]
  fetchData()
}

function renderChart() {
  // 空数据时容器被 v-show 隐藏，不初始化图表
  if (!chartRef.value || !data.value.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  const dates = data.value.map((p) => p.date)
  chart.setOption(
    {
      color: ['#409eff', '#67c23a'],
      tooltip: {
        trigger: 'axis',
        formatter(params: unknown) {
          const arr = params as Array<{ axisValue: string; value: string | number; marker: string; seriesName: string }>
          if (!arr || !arr.length) return ''
          const lines = [arr[0].axisValue]
          arr.forEach((p) => {
            lines.push(`${p.marker}${p.seriesName}：${p.value}`)
          })
          return lines.join('<br/>')
        },
      },
      legend: { data: ['缴费人数', '缴费金额'] },
      grid: { left: 16, right: 16, top: 48, bottom: 16, containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: dates },
      yAxis: [
        { type: 'value', name: '人数(人)', axisLabel: { formatter: '{value}' } },
        {
          type: 'value',
          name: '金额(元)',
          axisLabel: { formatter: (v: number) => Number(v).toLocaleString() },
        },
      ],
      series: [
        {
          name: '缴费人数',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: data.value.map((p) => p.payerCount),
        },
        {
          name: '缴费金额',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          showSymbol: false,
          areaStyle: { opacity: 0.1 },
          data: data.value.map((p) => p.amount),
        },
      ],
    },
    { notMerge: true },
  )
  chart.resize()
}

function onResize() {
  chart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = undefined
})
</script>

<style scoped>
.fee-trend-page { display: flex; flex-direction: column; gap: 16px; }
.toolbar-form { margin-bottom: 0; }
.chart-card { min-height: 480px; }
.chart-summary { display: flex; gap: 48px; padding: 8px 8px 4px; margin-bottom: 12px; }
.summary-item { display: flex; flex-direction: column; }
.summary-item .label { font-size: 13px; color: #909399; }
.summary-item .value { font-size: 26px; font-weight: bold; color: #409eff; line-height: 1.4; }
.summary-item .value.money { color: #67c23a; }
.chart { width: 100%; height: 420px; }
</style>