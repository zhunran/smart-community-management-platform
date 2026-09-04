<template>
  <div class="visitor-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>访客记录</span>
          <el-button type="primary" @click="openVerify">核销通行码</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in VISITOR_PASS_STATUS_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="姓名/手机号/车牌"
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="passCode" label="通行码" width="110" align="center" />
        <el-table-column prop="visitorName" label="访客姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="visitorPhone" label="手机号" width="140" align="center" />
        <el-table-column prop="plateNo" label="车牌" width="120" align="center">
          <template #default="{ row }">{{ row.plateNo || "-" }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="180" align="center">
          <template #default="{ row }">{{ fmt(row.validFrom) }} ~ {{ fmt(row.validUntil) }}</template>
        </el-table-column>
        <el-table-column label="次数" width="90" align="center">
          <template #default="{ row }">
            {{ row.usedCount }} / {{ row.maxUse === 0 ? "不限" : row.maxUse }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{
              row.statusName || VISITOR_PASS_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
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

    <!-- 核销弹窗 -->
    <el-dialog v-model="verifyVisible" title="核销通行码" width="440px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="通行码" required>
          <el-input
            v-model="verifyCode"
            placeholder="请输入6位数字通行码"
            maxlength="16"
            @keyup.enter="doVerify"
          />
        </el-form-item>
      </el-form>
      <div v-if="verifyResult" class="verify-result">
        <el-result
          :icon="verifyResult.valid ? 'success' : 'error'"
          :title="verifyResult.message"
          :sub-title="verifyResult.valid ? verifySubTitle : undefined"
        />
      </div>
      <template #footer>
        <el-button @click="verifyVisible = false">关闭</el-button>
        <el-button type="primary" :loading="verifyLoading" @click="doVerify">核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage } from "element-plus";
import { pageVisitorPass, verifyVisitorPass, VISITOR_PASS_STATUS_MAP } from "@/api/service";
import type { VisitorPassVO, VisitorPassVerifyVO } from "@/api/service";

function statusTagType(s: number) {
  const m: Record<number, string> = { 0: "success", 1: "warning", 2: "info", 3: "danger" };
  return m[s] || "info";
}
function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}

const loading = ref(false);
const tableData = ref<VisitorPassVO[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  status: undefined as number | undefined,
  keyword: undefined as string | undefined,
});

onMounted(() => fetchData());

async function fetchData() {
  loading.value = true;
  try {
    const res = await pageVisitorPass(query);
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
  query.keyword = undefined;
  search();
}

// ===== 核销 =====
const verifyVisible = ref(false);
const verifyLoading = ref(false);
const verifyCode = ref("");
const verifyResult = ref<VisitorPassVerifyVO>();

const verifySubTitle = computed(() => {
  const r = verifyResult.value;
  if (!r) return "";
  const use = r.maxUse === 0 ? "不限" : `${r.usedCount} / ${r.maxUse}`;
  return `访客：${r.visitorName || "-"}　车牌：${r.plateNo || "-"}　已用：${use}`;
});

function openVerify() {
  verifyCode.value = "";
  verifyResult.value = undefined;
  verifyVisible.value = true;
}

async function doVerify() {
  if (!verifyCode.value.trim()) {
    ElMessage.warning("请输入通行码");
    return;
  }
  verifyLoading.value = true;
  try {
    const res = await verifyVisitorPass(verifyCode.value.trim());
    verifyResult.value = res.data;
    fetchData();
  } finally {
    verifyLoading.value = false;
  }
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
.verify-result {
  margin-top: 8px;
}
</style>
