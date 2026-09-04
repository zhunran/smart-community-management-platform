<template>
  <div class="my-repairs">
    <van-nav-bar
      title="我的工单"
      left-text="返回"
      left-arrow
      @click-left="router.back()"
    />

    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多工单了"
      @load="load"
    >
      <div v-for="r in list" :key="r.id" class="order-card">
        <div class="order-head">
          <span class="order-no">{{ r.orderNo }}</span>
          <van-tag :color="statusColor(r.status)" text-color="#fff" round>{{
            r.statusName
          }}</van-tag>
        </div>

        <div class="order-title">{{ r.title }}</div>
        <div class="order-meta">
          <van-tag plain type="primary">{{ r.categoryName }}</van-tag>
          <van-tag plain :type="urgencyType(r.urgency)">{{
            r.urgencyName
          }}</van-tag>
          <span class="order-time">{{ fmt(r.createTime) }}</span>
        </div>

        <!-- 8 态 → 5 步 -->
        <div v-if="r.status !== 6 && r.status !== 7" class="steps-box">
          <van-steps :active="activeStep(r.status)" active-color="#3b82f6">
            <van-step>已提交</van-step>
            <van-step>待处理</van-step>
            <van-step>处理中</van-step>
            <van-step>待评价</van-step>
            <van-step>已完成</van-step>
          </van-steps>
        </div>
        <div v-else class="closed-tip">
          {{ r.status === 6 ? "已驳回" : "已取消" }}
        </div>

        <div v-if="r.rejectReason" class="reason">
          驳回原因：{{ r.rejectReason }}
        </div>
        <div v-if="r.handleNote" class="reason">
          处理说明：{{ r.handleNote }}
        </div>
        <div v-if="r.rating" class="reason">
          我的评价：{{ "★".repeat(r.rating) }} {{ r.ratingComment || "" }}
        </div>

        <div class="order-actions">
          <van-button
            v-if="r.status === 0 || r.status === 1"
            size="small"
            plain
            type="danger"
            round
            @click="handleCancel(r)"
          >
            取消工单
          </van-button>
          <van-button
            v-if="r.status === 4"
            size="small"
            type="primary"
            round
            @click="openRate(r)"
            >评价</van-button
          >
        </div>
      </div>
    </van-list>

    <van-empty v-if="!loading && list.length === 0" description="暂无工单" />

    <!-- 评价弹窗 -->
    <van-popup v-model:show="rateVisible" position="bottom" round>
      <div class="rate-panel">
        <div class="rate-title">评价服务</div>
        <van-rate
          v-model="rateForm.rating"
          :size="28"
          color="#ff9900"
          void-icon="star"
          void-color="#e2e8f0"
        />
        <van-field
          v-model="rateForm.ratingComment"
          type="textarea"
          rows="3"
          autosize
          maxlength="200"
          show-word-limit
          placeholder="说说维修师傅的服务如何…"
        />
        <van-button
          type="primary"
          block
          round
          :loading="rateLoading"
          @click="submitRate"
          >提交评价</van-button
        >
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { showConfirmDialog, showSuccessToast } from "vant";
import { myRepairs, cancelRepair, rateRepair } from "@/api/service";
import type { RepairOrderVO } from "@/api/service";

const router = useRouter();

function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function statusColor(s: number) {
  const m: Record<number, string> = {
    0: "#f59e0b",
    1: "#3b82f6",
    2: "#8b5cf6",
    3: "#06b6d4",
    4: "#22c55e",
    5: "#16a34a",
    6: "#ef4444",
    7: "#94a3b8",
  };
  return m[s] || "#94a3b8";
}
function urgencyType(u: number): "default" | "warning" | "danger" {
  const m: Record<number, "default" | "warning" | "danger"> = {
    1: "default",
    2: "warning",
    3: "danger",
  };
  return m[u] || "default";
}
function activeStep(s: number) {
  const m: Record<number, number> = { 0: 0, 1: 0, 2: 1, 3: 2, 4: 3, 5: 4 };
  return m[s] ?? 0;
}

const list = ref<RepairOrderVO[]>([]);
const loading = ref(false);
const finished = ref(false);
const page = ref(1);

async function load() {
  const res = await myRepairs({ current: page.value, size: 10 });
  const records = res.data.records || [];
  list.value.push(...records);
  finished.value = list.value.length >= Number(res.data.total);
  page.value += 1;
  loading.value = false;
}

function handleCancel(r: RepairOrderVO) {
  showConfirmDialog({ title: "提示", message: "确认取消该工单？" })
    .then(async () => {
      await cancelRepair(r.id);
      showSuccessToast("已取消");
      list.value = [];
      page.value = 1;
      finished.value = false;
      loading.value = true;
      load();
    })
    .catch(() => {});
}

// 评价
const rateVisible = ref(false);
const rateLoading = ref(false);
const current = ref<RepairOrderVO>();
const rateForm = reactive({ rating: 5, ratingComment: "" });

function openRate(r: RepairOrderVO) {
  current.value = r;
  rateForm.rating = 5;
  rateForm.ratingComment = "";
  rateVisible.value = true;
}

async function submitRate() {
  if (!current.value) return;
  rateLoading.value = true;
  try {
    await rateRepair(current.value.id, {
      rating: rateForm.rating,
      ratingComment: rateForm.ratingComment || undefined,
    });
    showSuccessToast("评价成功");
    rateVisible.value = false;
    list.value = [];
    page.value = 1;
    finished.value = false;
    loading.value = true;
    load();
  } finally {
    rateLoading.value = false;
  }
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.my-repairs {
  min-height: 100vh;
  background: #f5f7fa;
}
.order-card {
  background: #fff;
  border-radius: 12px;
  margin: 12px;
  padding: 14px;
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-no {
  font-size: 12px;
  color: #94a3b8;
}
.order-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.order-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.order-time {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}
.steps-box {
  padding: 6px 0;
}
.closed-tip {
  font-size: 13px;
  color: #94a3b8;
  padding: 8px 0;
}
.reason {
  font-size: 13px;
  color: #64748b;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}
.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
.rate-panel {
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
}
.rate-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  text-align: center;
  margin-bottom: 12px;
}
.rate-panel .van-rate {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}
</style>
