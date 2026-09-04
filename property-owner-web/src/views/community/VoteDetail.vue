<template>
  <div class="vote-detail">
    <van-nav-bar
      title="投票详情"
      left-text="返回"
      left-arrow
      @click-left="router.back()"
    />

    <div v-if="loading" class="loading-box">
      <van-skeleton title :row="5" />
    </div>

    <template v-else-if="detail">
      <div class="body">
        <div class="head-card">
          <div class="title-row">
            <span class="title">{{ detail.title }}</span>
            <van-tag
              :color="statusColor(detail.status)"
              text-color="#fff"
              round
              >{{ detail.statusName }}</van-tag
            >
          </div>
          <div class="meta-row">
            <van-tag plain type="primary">{{ detail.voteTypeName }}</van-tag>
            <van-tag
              plain
              :type="detail.isAnonymous === 1 ? 'default' : 'warning'"
            >
              {{ detail.isAnonymous === 1 ? "匿名" : "实名" }}
            </van-tag>
          </div>
          <div class="meta-item">
            <van-icon name="clock-o" />
            <span>{{ fmt(detail.startTime) }} ~ {{ fmt(detail.endTime) }}</span>
          </div>
          <div v-if="detail.description" class="desc">
            {{ detail.description }}
          </div>
        </div>

        <!-- 未开始 -->
        <div v-if="detail.status === 0" class="tip-card">
          投票尚未开始，敬请期待
        </div>

        <!-- 投票中：未投 -->
        <div v-else-if="detail.status === 1 && !voted" class="vote-card">
          <div class="section-title">
            {{
              detail.voteType === 1 ? "请选择一个选项" : "请选择一个或多个选项"
            }}
          </div>
          <van-radio-group
            v-if="detail.voteType === 1"
            v-model="singleId"
            class="option-list"
          >
            <div v-for="o in detail.options" :key="o.id" class="option-item">
              <van-radio :name="o.id">{{ o.content }}</van-radio>
            </div>
          </van-radio-group>
          <van-checkbox-group v-else v-model="selectedIds" class="option-list">
            <div v-for="o in detail.options" :key="o.id" class="option-item">
              <van-checkbox :name="o.id">{{ o.content }}</van-checkbox>
            </div>
          </van-checkbox-group>
          <van-button
            type="primary"
            block
            round
            :loading="actionLoading"
            class="submit-btn"
            @click="submit"
          >
            提交投票
          </van-button>
        </div>

        <!-- 已投 / 已结束：结果 -->
        <div v-else class="result-card">
          <div class="result-head">
            <span class="section-title">投票结果</span>
            <van-tag v-if="detail.status === 1 && voted" type="success"
              >已投票</van-tag
            >
          </div>
          <div class="total-tip">共 {{ detail.totalVotes ?? 0 }} 票</div>
          <div v-for="o in detail.options" :key="o.id" class="result-item">
            <div class="result-label">
              <span class="result-name">{{ o.content }}</span>
              <span class="result-count"
                >{{ o.voteCount }} 票 · {{ percent(o.voteCount) }}%</span
              >
            </div>
            <div class="result-bar">
              <div
                class="result-inner"
                :style="{ width: percent(o.voteCount) + '%' }"
              />
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { showSuccessToast, showToast } from "vant";
import { getVote, castVote } from "@/api/community";
import type { VoteDetailVO } from "@/api/community";

const route = useRoute();
const router = useRouter();
const id = String(route.params.id);

function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function statusColor(s: number) {
  const m: Record<number, string> = {
    0: "#94a3b8",
    1: "#3b82f6",
    2: "#22c55e",
  };
  return m[s] || "#94a3b8";
}

const loading = ref(false);
const detail = ref<VoteDetailVO>();
const actionLoading = ref(false);
const singleId = ref("");
const selectedIds = ref<string[]>([]);

const voted = computed(() => (detail.value?.myVotedOptionIds?.length ?? 0) > 0);

function percent(count: number) {
  const total = detail.value?.totalVotes ?? 0;
  if (!total) return 0;
  return Math.round((count / total) * 100);
}

async function load() {
  loading.value = true;
  try {
    const res = await getVote(id);
    detail.value = res.data;
    const votedIds = res.data.myVotedOptionIds || [];
    if (votedIds.length) {
      if (res.data.voteType === 1) singleId.value = votedIds[0];
      else selectedIds.value = votedIds;
    }
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (!detail.value) return;
  const ids =
    detail.value.voteType === 1
      ? singleId.value
        ? [singleId.value]
        : []
      : selectedIds.value;
  if (!ids.length) {
    showToast(
      detail.value.voteType === 1 ? "请选择一个选项" : "请至少选择一个选项",
    );
    return;
  }
  actionLoading.value = true;
  try {
    await castVote(id, ids);
    showSuccessToast("投票成功");
    await load();
  } finally {
    actionLoading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.vote-detail {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 30px;
}
.loading-box {
  padding: 20px;
}
.body {
  padding: 16px;
}
.head-card,
.vote-card,
.result-card,
.tip-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
}
.meta-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}
.meta-item .van-icon {
  color: #3b82f6;
}
.desc {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-top: 4px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}
.option-list {
  margin-bottom: 16px;
}
.option-item {
  padding: 6px 0;
}
.option-item :deep(.van-radio__label),
.option-item :deep(.van-checkbox__label) {
  font-size: 15px;
  color: #334155;
}
.submit-btn {
  margin-top: 4px;
}
.tip-card {
  text-align: center;
  font-size: 14px;
  color: #94a3b8;
  padding: 28px 16px;
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.total-tip {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 14px;
}
.result-item {
  margin-bottom: 14px;
}
.result-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.result-name {
  font-size: 14px;
  color: #334155;
}
.result-count {
  font-size: 12px;
  color: #64748b;
}
.result-bar {
  height: 8px;
  background: #eef2f7;
  border-radius: 4px;
  overflow: hidden;
}
.result-inner {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 4px;
  transition: width 0.5s ease;
}
</style>
