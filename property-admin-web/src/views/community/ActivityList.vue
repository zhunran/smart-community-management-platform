<template>
  <div class="activity-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <div>
            <el-button type="primary" @click="handleAdd">新建活动</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="标题">
          <el-input
            v-model="query.title"
            clearable
            placeholder="活动标题"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="query.activityType"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="(l, k) in ACTIVITY_TYPE_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in ACTIVITY_STATUS_MAP"
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

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column label="封面" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImage"
              :src="row.coverImage"
              :preview-src-list="[row.coverImage]"
              preview-teleported
              fit="cover"
              class="cover-img"
            />
            <span v-else class="no-cover">无</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="活动标题"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              row.activityTypeName || ACTIVITY_TYPE_MAP[row.activityType]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{
              ACTIVITY_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="location"
          label="地点"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column label="活动时间" width="170" align="center">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="报名人数" width="110" align="center">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="showDetail(row)">
              {{ row.signupCount }} / {{ row.maxParticipants }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              size="small"
              type="success"
              @click="handlePublish(row)"
              >发布</el-button
            >
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              type="primary"
              @click="handleEdit(row)"
              >编辑</el-button
            >
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              type="warning"
              @click="handleCancel(row)"
              >取消</el-button
            >
            <el-button
              v-if="row.status === 0"
              size="small"
              type="danger"
              @click="handleDelete(row)"
              >删除</el-button
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

    <!-- 新建 / 编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑活动' : '新建活动'"
      size="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            placeholder="请输入活动标题"
          />
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="form.activityType" style="width: 100%">
            <el-option
              v-for="(l, k) in ACTIVITY_TYPE_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图URL">
          <el-input
            v-model="form.coverImage"
            placeholder="请输入封面图片 URL（可留空）"
          />
        </el-form-item>
        <el-form-item label="活动地点" prop="location">
          <el-input
            v-model="form.location"
            maxlength="200"
            placeholder="请输入活动地点"
          />
        </el-form-item>
        <el-form-item label="组织者" prop="organizer">
          <el-input
            v-model="form.organizer"
            maxlength="100"
            placeholder="请输入组织者"
          />
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
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
        <el-form-item label="报名时间">
          <el-date-picker
            v-model="form.signupRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="报名开始"
            end-placeholder="报名截止"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大人数" prop="maxParticipants">
          <el-input-number
            v-model="form.maxParticipants"
            :min="1"
            :max="100000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="5000"
            show-word-limit
            placeholder="请输入活动内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button
          v-if="!isEdit"
          :loading="submitLoading"
          @click="handleSubmit(false)"
          >保存草稿</el-button
        >
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit(true)"
          >{{ isEdit ? "保存" : "直接发布" }}</el-button
        >
      </template>
    </el-drawer>

    <!-- 活动详情 -->
    <el-dialog v-model="detailVisible" title="活动详情" width="560px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题" :span="2">{{
          detail?.title
        }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{
          detail?.activityTypeName
        }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="detail ? statusTagType(detail.status) : 'info'"
            size="small"
          >
            {{ detail ? ACTIVITY_STATUS_MAP[detail.status] : "-" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地点">{{
          detail?.location
        }}</el-descriptions-item>
        <el-descriptions-item label="组织者">{{
          detail?.organizer
        }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{
          fmt(detail?.startTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{
          fmt(detail?.endTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="报名人数"
          >{{ detail?.signupCount }} /
          {{ detail?.maxParticipants }}</el-descriptions-item
        >
        <el-descriptions-item label="内容" :span="2">{{
          detail?.content
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  pageActivity,
  getActivity,
  createActivity,
  createAndPublishActivity,
  updateActivity,
  deleteActivity,
  publishActivity,
  cancelActivity,
  ACTIVITY_STATUS_MAP,
  ACTIVITY_TYPE_MAP,
} from "@/api/community";
import type {
  CommunityActivityVO,
  CommunityActivityDetailVO,
  CommunityActivityCreateRequest,
} from "@/api/community";

function statusTagType(s: number) {
  const m: Record<number, string> = {
    0: "info",
    1: "primary",
    2: "warning",
    3: "success",
    4: "info",
    5: "danger",
  };
  return m[s] || "info";
}

function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}

const loading = ref(false);
const tableData = ref<CommunityActivityVO[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  title: undefined as string | undefined,
  activityType: undefined as number | undefined,
  status: undefined as number | undefined,
});

onMounted(() => fetchData());

async function fetchData() {
  loading.value = true;
  try {
    const res = await pageActivity(query);
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
  query.activityType = undefined;
  query.status = undefined;
  search();
}

// ===== 表单 =====
const drawerVisible = ref(false);
const submitLoading = ref(false);
const isEdit = ref(false);
const editId = ref("");
const formRef = ref<FormInstance>();
const form = reactive({
  title: "",
  content: "",
  coverImage: "",
  activityType: 1 as number,
  location: "",
  organizer: "",
  timeRange: [] as string[],
  signupRange: [] as string[],
  maxParticipants: 50,
});
const rules: FormRules = {
  title: [{ required: true, message: "请输入活动标题", trigger: "blur" }],
  activityType: [
    { required: true, message: "请选择活动类型", trigger: "change" },
  ],
  location: [{ required: true, message: "请输入活动地点", trigger: "blur" }],
  organizer: [{ required: true, message: "请输入组织者", trigger: "blur" }],
  timeRange: [{ required: true, message: "请选择活动时间", trigger: "change" }],
  content: [{ required: true, message: "请输入活动内容", trigger: "blur" }],
};

function handleAdd() {
  isEdit.value = false;
  editId.value = "";
  Object.assign(form, {
    title: "",
    content: "",
    coverImage: "",
    activityType: 1,
    location: "",
    organizer: "",
    timeRange: [],
    signupRange: [],
    maxParticipants: 50,
  });
  drawerVisible.value = true;
}

async function handleEdit(row: CommunityActivityVO) {
  const res = await getActivity(row.id);
  const d = res.data as CommunityActivityDetailVO;
  isEdit.value = true;
  editId.value = row.id;
  Object.assign(form, {
    title: d.title,
    content: d.content || "",
    coverImage: d.coverImage || "",
    activityType: d.activityType,
    location: d.location,
    organizer: d.organizer,
    timeRange: [d.startTime, d.endTime],
    signupRange:
      d.signupStart && d.signupEnd ? [d.signupStart, d.signupEnd] : [],
    maxParticipants: d.maxParticipants,
  });
  drawerVisible.value = true;
}

async function handleSubmit(publish: boolean) {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  const payload: CommunityActivityCreateRequest = {
    title: form.title,
    content: form.content,
    coverImage: form.coverImage || undefined,
    activityType: form.activityType,
    location: form.location,
    organizer: form.organizer,
    startTime: form.timeRange[0],
    endTime: form.timeRange[1],
    signupStart: form.signupRange[0] || undefined,
    signupEnd: form.signupRange[1] || undefined,
    maxParticipants: form.maxParticipants,
  };
  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await updateActivity({ ...payload, id: editId.value });
      ElMessage.success("修改成功");
    } else if (publish) {
      await createAndPublishActivity(payload);
      ElMessage.success("发布成功");
    } else {
      await createActivity(payload);
      ElMessage.success("创建成功");
    }
    drawerVisible.value = false;
    fetchData();
  } finally {
    submitLoading.value = false;
  }
}

// ===== 操作 =====
function handlePublish(row: CommunityActivityVO) {
  ElMessageBox.confirm(`确认发布活动「${row.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "success",
  })
    .then(async () => {
      await publishActivity(row.id);
      ElMessage.success("已发布");
      fetchData();
    })
    .catch(() => {});
}

function handleCancel(row: CommunityActivityVO) {
  ElMessageBox.confirm(
    `确认取消活动「${row.title}」？取消后将通知已报名业主。`,
    "提示",
    { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" },
  )
    .then(async () => {
      await cancelActivity(row.id);
      ElMessage.success("已取消");
      fetchData();
    })
    .catch(() => {});
}

function handleDelete(row: CommunityActivityVO) {
  ElMessageBox.confirm(
    `确认删除活动「${row.title}」？此操作不可恢复。`,
    "提示",
    { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" },
  )
    .then(async () => {
      await deleteActivity(row.id);
      ElMessage.success("已删除");
      fetchData();
    })
    .catch(() => {});
}

// ===== 详情 =====
const detailVisible = ref(false);
const detail = ref<CommunityActivityDetailVO>();

async function showDetail(row: CommunityActivityVO) {
  const res = await getActivity(row.id);
  detail.value = res.data;
  detailVisible.value = true;
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
.cover-img {
  width: 44px;
  height: 44px;
  border-radius: 6px;
}
.no-cover {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
