<template>
  <div class="repair-list">
    <!-- 统计图表 -->
    <div class="stats-row">
      <el-card shadow="never" class="summary-card">
        <div class="summary-item">
          <span class="summary-num">{{ stats.total ?? 0 }}</span>
          <span class="summary-label">工单总数</span>
        </div>
        <div class="summary-item">
          <span class="summary-num">{{ avgHours }}</span>
          <span class="summary-label">平均处理时长(小时)</span>
        </div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <div class="chart-title">工单状态分布</div>
        <div ref="pieRef" class="chart" v-loading="statsLoading" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <div class="chart-title">各状态数量</div>
        <div ref="lineRef" class="chart" v-loading="statsLoading" />
      </el-card>
    </div>

    <!-- 工单列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>报修工单</span>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in REPAIR_STATUS_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in REPAIR_CATEGORY_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="超时">
          <el-select v-model="query.timeoutFlag" clearable style="width: 110px">
            <el-option label="已超时" :value="1" />
            <el-option label="正常" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="标题/工单号"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :row-class-name="rowClass"
      >
        <el-table-column
          prop="orderNo"
          label="工单号"
          width="180"
          align="center"
        />
        <el-table-column
          prop="title"
          label="问题描述"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              row.categoryName || REPAIR_CATEGORY_MAP[row.category]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="紧急" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="urgencyTagType(row.urgency)" size="small">{{
              row.urgencyName || URGENCY_MAP[row.urgency]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{
              row.statusName || REPAIR_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超时" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.timeoutFlag === 1" type="danger" size="small"
              >超时</el-tag
            >
            <span v-else class="normal">-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="openAudit(row, true)"
              >通过</el-button
            >
            <el-button
              v-if="row.status === 0"
              size="small"
              type="warning"
              @click="openAudit(row, false)"
              >驳回</el-button
            >
            <el-button
              v-if="row.status === 1"
              size="small"
              type="primary"
              @click="openAssign(row)"
              >指派</el-button
            >
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="工单详情" size="520px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="工单号">{{
          detail.orderNo
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" size="small">{{
            detail.statusName || REPAIR_STATUS_MAP[detail.status]
          }}</el-tag>
          <el-tag
            v-if="detail.timeoutFlag === 1"
            type="danger"
            size="small"
            style="margin-left: 8px"
            >已超时</el-tag
          >
        </el-descriptions-item>
        <el-descriptions-item label="分类">{{
          detail.categoryName || REPAIR_CATEGORY_MAP[detail.category]
        }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="urgencyTagType(detail.urgency)" size="small">{{
            detail.urgencyName || URGENCY_MAP[detail.urgency]
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述">{{
          detail.title
        }}</el-descriptions-item>
        <el-descriptions-item label="问题详情">{{
          detail.description
        }}</el-descriptions-item>
        <el-descriptions-item label="现场照片">
          <div v-if="imageList.length" class="img-list">
            <el-image
              v-for="(img, i) in imageList"
              :key="i"
              :src="img"
              :preview-src-list="imageList"
              :initial-index="i"
              preview-teleported
              fit="cover"
              class="detail-img"
            />
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.handlerId" label="维修员ID">{{
          detail.handlerId
        }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.handleNote" label="处理说明">{{
          detail.handleNote
        }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因">{{
          detail.rejectReason
        }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rating" label="评价"
          >{{ detail.rating }} 星</el-descriptions-item
        >
        <el-descriptions-item v-if="detail.ratingComment" label="评价内容">{{
          detail.ratingComment
        }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          fmt(detail.createTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{
          fmt(detail.updateTime)
        }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="auditVisible"
      :title="auditPass ? '通过审核' : '驳回工单'"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="audit-title">工单：{{ current?.orderNo }}</div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item v-if="!auditPass" label="驳回原因" required>
          <el-input
            v-model="auditReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入驳回原因"
          />
        </el-form-item>
        <el-form-item v-else>
          <div class="audit-tip">确认通过该工单？通过后将进入待派单状态。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="doAudit"
          >确定</el-button
        >
      </template>
    </el-dialog>

    <!-- 指派弹窗 -->
    <el-dialog
      v-model="assignVisible"
      title="指派维修员"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="维修员ID" required>
          <el-input v-model="handlerId" placeholder="请输入维修员ID（数字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="doAssign"
          >确定</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  reactive,
  onMounted,
  onBeforeUnmount,
  nextTick,
  computed,
} from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import * as echarts from "echarts/core";
import { PieChart, LineChart } from "echarts/charts";
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { LegacyGridContainLabel } from "echarts/features";
import type { ECharts } from "echarts/core";
import {
  pageRepair,
  getRepair,
  auditRepair,
  assignRepair,
  repairStatistics,
  REPAIR_STATUS_MAP,
  REPAIR_CATEGORY_MAP,
  URGENCY_MAP,
} from "@/api/service";
import type { RepairOrderVO, RepairStatisticsVO } from "@/api/service";

echarts.use([
  PieChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CanvasRenderer,
  LegacyGridContainLabel,
]);

function statusTagType(s: number) {
  const m: Record<number, string> = {
    0: "warning",
    1: "primary",
    2: "info",
    3: "info",
    4: "success",
    5: "success",
    6: "danger",
    7: "info",
  };
  return m[s] || "info";
}
function urgencyTagType(u: number) {
  const m: Record<number, string> = { 1: "info", 2: "warning", 3: "danger" };
  return m[u] || "info";
}
function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function rowClass({ row }: { row: RepairOrderVO }) {
  return row.timeoutFlag === 1 ? "timeout-row" : "";
}

const loading = ref(false);
const tableData = ref<RepairOrderVO[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  status: undefined as number | undefined,
  category: undefined as number | undefined,
  timeoutFlag: undefined as number | undefined,
  keyword: undefined as string | undefined,
});

onMounted(() => {
  fetchData();
  fetchStats();
  window.addEventListener("resize", onResize);
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", onResize);
  pieChart?.dispose();
  lineChart?.dispose();
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await pageRepair(query);
    tableData.value = (res?.data?.records || []).filter(Boolean);
    total.value = Number(res?.data?.total || 0);
  } finally {
    loading.value = false;
  }
}
function search() {
  query.current = 1;
  fetchData();
}
function reset() {
  query.status = undefined;
  query.category = undefined;
  query.timeoutFlag = undefined;
  query.keyword = undefined;
  search();
}

// ===== 统计图表 =====
const statsLoading = ref(false);
const stats = ref<RepairStatisticsVO>({
  total: 0,
  statusCounts: {},
  avgHandleHours: 0,
});
const pieRef = ref<HTMLElement>();
const lineRef = ref<HTMLElement>();
let pieChart: ECharts | undefined;
let lineChart: ECharts | undefined;

const avgHours = computed(() => {
  const v = stats.value.avgHandleHours;
  return v == null ? "-" : Number(v).toFixed(1);
});

async function fetchStats() {
  statsLoading.value = true;
  try {
    const res = await repairStatistics();
    stats.value = res.data || { total: 0, statusCounts: {}, avgHandleHours: 0 };
    await nextTick();
    renderCharts();
  } finally {
    statsLoading.value = false;
  }
}

function statusNames() {
  return [0, 1, 2, 3, 4, 5, 6, 7].map((i) => REPAIR_STATUS_MAP[i]);
}
function statusValues() {
  const c = stats.value.statusCounts || {};
  return [0, 1, 2, 3, 4, 5, 6, 7].map((i) => Number(c[i] || 0));
}

function renderCharts() {
  if (pieRef.value) {
    if (!pieChart) pieChart = echarts.init(pieRef.value);
    const values = statusValues();
    const data = [0, 1, 2, 3, 4, 5, 6, 7]
      .map((i) => ({ name: REPAIR_STATUS_MAP[i], value: values[i] }))
      .filter((d) => d.value > 0);
    pieChart.setOption(
      {
        color: [
          "#e6a23c",
          "#409eff",
          "#909399",
          "#606266",
          "#67c23a",
          "#95d475",
          "#f56c6c",
          "#c0c4cc",
        ],
        tooltip: { trigger: "item", formatter: "{b}：{c}（{d}%）" },
        legend: { bottom: 0, type: "scroll" },
        series: [
          {
            type: "pie",
            radius: ["40%", "65%"],
            center: ["50%", "45%"],
            data,
            label: { formatter: "{b}\n{c}" },
          },
        ],
      },
      { notMerge: true },
    );
    pieChart.resize();
  }
  if (lineRef.value) {
    if (!lineChart) lineChart = echarts.init(lineRef.value);
    lineChart.setOption(
      {
        color: ["#409eff"],
        tooltip: { trigger: "axis" },
        grid: { left: 16, right: 16, top: 24, bottom: 16, containLabel: true },
        xAxis: {
          type: "category",
          data: statusNames(),
          axisLabel: { interval: 0, rotate: 20 },
        },
        yAxis: { type: "value", minInterval: 1 },
        series: [
          {
            name: "数量",
            type: "line",
            smooth: true,
            symbol: "circle",
            symbolSize: 8,
            data: statusValues(),
            areaStyle: { opacity: 0.08 },
          },
        ],
      },
      { notMerge: true },
    );
    lineChart.resize();
  }
}
function onResize() {
  pieChart?.resize();
  lineChart?.resize();
}

// ===== 详情 =====
const detailVisible = ref(false);
const detail = ref<RepairOrderVO>();
const imageList = computed(() => {
  const img = detail.value?.images;
  if (!img) return [];
  return img
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
});

async function showDetail(row: RepairOrderVO) {
  const res = await getRepair(row.id);
  detail.value = res.data;
  detailVisible.value = true;
}

// ===== 审核 =====
const auditVisible = ref(false);
const auditPass = ref(true);
const auditLoading = ref(false);
const auditReason = ref("");
const current = ref<RepairOrderVO>();

function openAudit(row: RepairOrderVO, pass: boolean) {
  current.value = row;
  auditPass.value = pass;
  auditReason.value = "";
  auditVisible.value = true;
}

async function doAudit() {
  if (!current.value) return;
  if (!auditPass.value && !auditReason.value.trim()) {
    ElMessage.warning("请填写驳回原因");
    return;
  }
  auditLoading.value = true;
  try {
    await auditRepair(
      current.value.id,
      auditPass.value,
      auditPass.value ? undefined : auditReason.value,
    );
    ElMessage.success("审核完成");
    auditVisible.value = false;
    fetchData();
    fetchStats();
  } finally {
    auditLoading.value = false;
  }
}

// ===== 指派 =====
const assignVisible = ref(false);
const assignLoading = ref(false);
const handlerId = ref("");

function openAssign(row: RepairOrderVO) {
  current.value = row;
  handlerId.value = "";
  assignVisible.value = true;
}

async function doAssign() {
  if (!current.value) return;
  if (!handlerId.value.trim()) {
    ElMessage.warning("请输入维修员ID");
    return;
  }
  assignLoading.value = true;
  try {
    await assignRepair(current.value.id, handlerId.value.trim());
    ElMessage.success("派单成功");
    assignVisible.value = false;
    fetchData();
    fetchStats();
  } finally {
    assignLoading.value = false;
  }
}
</script>

<style scoped>
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.summary-card {
  flex: 0 0 220px;
}
.summary-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.summary-item {
  display: flex;
  flex-direction: column;
}
.summary-num {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  line-height: 1.2;
}
.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.chart-card {
  flex: 1;
  min-width: 0;
}
.chart-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.chart {
  width: 100%;
  height: 260px;
}
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
.normal {
  color: #c0c4cc;
}
.audit-title {
  color: #303133;
  font-weight: 600;
}
.audit-tip {
  color: #909399;
  line-height: 1.6;
}
.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
}
:deep(.timeout-row) {
  --el-table-tr-bg-color: #fef0f0;
}
</style>
