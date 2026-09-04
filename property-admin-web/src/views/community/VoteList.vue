<template>
  <div class="vote-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>投票管理</span>
          <el-button type="primary" @click="handleAdd">新建投票</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="标题">
          <el-input
            v-model="query.title"
            clearable
            placeholder="投票标题"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in VOTE_STATUS_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="title" label="投票标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              row.voteTypeName || VOTE_TYPE_MAP[row.voteType]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投票方式" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAnonymous === 1 ? 'info' : 'warning'" size="small">{{
              row.isAnonymous === 1 ? '匿名' : '实名'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{
              row.statusName || VOTE_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="handleStart(row)"
              >开始</el-button
            >
            <el-button
              v-if="row.status === 1"
              size="small"
              type="warning"
              @click="handleEnd(row)"
              >结束</el-button
            >
            <el-button size="small" type="primary" @click="showResult(row)">结果</el-button>
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

    <!-- 新建投票抽屉 -->
    <el-drawer v-model="drawerVisible" title="新建投票" size="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="投票标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入投票标题" />
        </el-form-item>
        <el-form-item label="投票描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入投票描述" />
        </el-form-item>
        <el-form-item label="投票类型" prop="voteType">
          <el-radio-group v-model="form.voteType">
            <el-radio-button :value="1">单选</el-radio-button>
            <el-radio-button :value="2">多选</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="投票方式" prop="isAnonymous">
          <el-radio-group v-model="form.isAnonymous">
            <el-radio-button :value="1">匿名</el-radio-button>
            <el-radio-button :value="0">实名</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="投票时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="投票选项" prop="options">
          <div class="option-list">
            <div v-for="(_, i) in form.options" :key="i" class="option-row">
              <el-input v-model="form.options[i]" :placeholder="`选项 ${i + 1}`" maxlength="200" />
              <el-button
                v-if="form.options.length > 2"
                type="danger"
                text
                @click="removeOption(i)"
                >删除</el-button
              >
            </div>
            <el-button type="primary" text @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">创建</el-button>
      </template>
    </el-drawer>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" title="投票结果" width="640px">
      <div v-if="result" class="result-body">
        <div class="result-head">
          <span class="result-title">{{ result.title }}</span>
          <el-tag :type="statusTagType(result.status)" size="small">{{
            result.statusName || VOTE_STATUS_MAP[result.status]
          }}</el-tag>
        </div>
        <div class="result-total">总票数：{{ result.totalVotes ?? 0 }}</div>
        <div ref="barRef" class="result-chart" v-loading="resultLoading" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import * as echarts from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import type { ECharts } from "echarts/core";
import {
  pageVote,
  getVote,
  createVote,
  startVote,
  endVote,
  VOTE_STATUS_MAP,
  VOTE_TYPE_MAP,
} from "@/api/community";
import type { VoteVO, VoteDetailVO, VoteCreateRequest } from "@/api/community";

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

function statusTagType(s: number) {
  const m: Record<number, string> = { 0: "info", 1: "primary", 2: "success" };
  return m[s] || "info";
}
function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}

const loading = ref(false);
const tableData = ref<VoteVO[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  title: undefined as string | undefined,
  status: undefined as number | undefined,
});

onMounted(() => fetchData());

async function fetchData() {
  loading.value = true;
  try {
    const res = await pageVote(query);
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
  query.title = undefined;
  query.status = undefined;
  search();
}

// ===== 新建 =====
const drawerVisible = ref(false);
const submitLoading = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  title: "",
  description: "",
  voteType: 1 as number,
  isAnonymous: 1 as number,
  timeRange: [] as string[],
  options: ["", ""] as string[],
});
const rules: FormRules = {
  title: [{ required: true, message: "请输入投票标题", trigger: "blur" }],
  voteType: [{ required: true, message: "请选择投票类型", trigger: "change" }],
  isAnonymous: [{ required: true, message: "请选择投票方式", trigger: "change" }],
  timeRange: [{ required: true, message: "请选择投票时间", trigger: "change" }],
};

function handleAdd() {
  Object.assign(form, {
    title: "",
    description: "",
    voteType: 1,
    isAnonymous: 1,
    timeRange: [],
    options: ["", ""],
  });
  drawerVisible.value = true;
}

function addOption() {
  form.options.push("");
}
function removeOption(i: number) {
  form.options.splice(i, 1);
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  const options = form.options.map((o) => o.trim()).filter(Boolean);
  if (options.length < 2) {
    ElMessage.warning("请至少填写两个选项");
    return;
  }
  const payload: VoteCreateRequest = {
    title: form.title,
    description: form.description || undefined,
    voteType: form.voteType,
    isAnonymous: form.isAnonymous,
    startTime: form.timeRange[0],
    endTime: form.timeRange[1],
    options,
  };
  submitLoading.value = true;
  try {
    await createVote(payload);
    ElMessage.success("创建成功");
    drawerVisible.value = false;
    fetchData();
  } finally {
    submitLoading.value = false;
  }
}

// ===== 开始/结束 =====
function handleStart(row: VoteVO) {
  ElMessageBox.confirm(`确认开始投票「${row.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "success",
  })
    .then(async () => {
      await startVote(row.id);
      ElMessage.success("已开始");
      fetchData();
    })
    .catch(() => {});
}
function handleEnd(row: VoteVO) {
  ElMessageBox.confirm(`确认结束投票「${row.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      await endVote(row.id);
      ElMessage.success("已结束");
      fetchData();
    })
    .catch(() => {});
}

// ===== 结果 =====
const resultVisible = ref(false);
const resultLoading = ref(false);
const result = ref<VoteDetailVO>();
const barRef = ref<HTMLElement>();
let barChart: ECharts | undefined;

onBeforeUnmount(() => barChart?.dispose());

async function showResult(row: VoteVO) {
  resultVisible.value = true;
  result.value = undefined;
  resultLoading.value = true;
  try {
    const res = await getVote(row.id);
    result.value = res.data;
    await nextTick();
    renderChart();
  } finally {
    resultLoading.value = false;
  }
}

function renderChart() {
  if (!barRef.value || !result.value) return;
  if (!barChart) barChart = echarts.init(barRef.value);
  const options = result.value.options || [];
  barChart.setOption(
    {
      color: ["#409eff"],
      tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
      grid: { left: 16, right: 40, top: 16, bottom: 16, containLabel: true },
      xAxis: { type: "value", minInterval: 1 },
      yAxis: {
        type: "category",
        data: options.map((o) => o.content),
        axisLabel: { interval: 0 },
      },
      series: [
        {
          name: "票数",
          type: "bar",
          barMaxWidth: 28,
          data: options.map((o) => o.voteCount),
          label: { show: true, position: "right" },
        },
      ],
    },
    { notMerge: true },
  );
  barChart.resize();
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
.option-list {
  width: 100%;
}
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.result-body {
  display: flex;
  flex-direction: column;
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.result-total {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}
.result-chart {
  width: 100%;
  height: 320px;
}
</style>
