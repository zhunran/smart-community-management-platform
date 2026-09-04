<template>
  <div class="venue-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>场地管理</span>
          <el-button type="primary" @click="handleAdd">新建场地</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="名称">
          <el-input
            v-model="query.name"
            clearable
            placeholder="场地名称"
            style="width: 160px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.venueType" clearable style="width: 130px">
            <el-option
              v-for="(l, k) in VENUE_TYPE_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 110px">
            <el-option
              v-for="(l, k) in VENUE_STATUS_MAP"
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
        <el-table-column prop="name" label="场地名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{
              row.venueTypeName || VENUE_TYPE_MAP[row.venueType]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column label="开放时间" width="120" align="center">
          <template #default="{ row }">{{ fmtTime(row.openTime) }} ~ {{ fmtTime(row.closeTime) }}</template>
        </el-table-column>
        <el-table-column prop="slotMinutes" label="粒度(分)" width="90" align="center" />
        <el-table-column label="月上限" width="80" align="center">
          <template #default="{ row }">{{ row.monthlyLimit === 0 ? "不限" : row.monthlyLimit }}</template>
        </el-table-column>
        <el-table-column label="费用" width="90" align="center">
          <template #default="{ row }">¥{{ Number(row.price ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{
              row.statusName || VENUE_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="showBookings(row)">预约记录</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑场地' : '新建场地'"
      size="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="场地名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="请输入场地名称" />
        </el-form-item>
        <el-form-item label="场地类型" prop="venueType">
          <el-select v-model="form.venueType" style="width: 100%">
            <el-option
              v-for="(l, k) in VENUE_TYPE_MAP"
              :key="k"
              :label="l"
              :value="Number(k)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="场地位置">
          <el-input v-model="form.location" maxlength="200" placeholder="请输入场地位置" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="100000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开放时间" prop="openTime">
          <el-time-picker v-model="form.openTime" value-format="HH:mm:ss" format="HH:mm" placeholder="开放时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="关闭时间" prop="closeTime">
          <el-time-picker v-model="form.closeTime" value-format="HH:mm:ss" format="HH:mm" placeholder="关闭时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预约粒度(分)" prop="slotMinutes">
          <el-input-number v-model="form.slotMinutes" :min="10" :max="480" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月度上限">
          <el-input-number v-model="form.monthlyLimit" :min="0" :max="1000" style="width: 100%" />
          <div class="form-tip">0 表示不限</div>
        </el-form-item>
        <el-form-item label="费用(元)">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-drawer>

    <!-- 预约记录弹窗 -->
    <el-dialog v-model="bookingVisible" :title="`预约记录 - ${currentVenueName}`" width="720px">
      <el-table :data="bookingData" v-loading="bookingLoading" stripe border style="width: 100%">
        <el-table-column prop="venueName" label="场地" min-width="120" show-overflow-tooltip />
        <el-table-column prop="bookingDate" label="日期" width="110" align="center" />
        <el-table-column label="时段" width="140" align="center">
          <template #default="{ row }">{{ fmtTime(row.startTime) }} ~ {{ fmtTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="bookingTagType(row.status)" size="small">{{
              row.statusName || BOOKING_STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="bookingQuery.current"
          v-model:page-size="bookingQuery.size"
          :total="bookingTotal"
          :page-sizes="[10, 20, 50]"
          layout="total,sizes,prev,pager,next,jumper"
          @change="fetchBookings"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  pageVenue,
  getVenue,
  createVenue,
  updateVenue,
  deleteVenue,
  pageVenueBooking,
  VENUE_TYPE_MAP,
  VENUE_STATUS_MAP,
  BOOKING_STATUS_MAP,
} from "@/api/service";
import type { VenueVO, VenueBookingVO, VenueCreateRequest } from "@/api/service";

function fmtTime(s?: string) {
  if (!s) return "-";
  return s.slice(0, 5);
}
function fmt(s?: string) {
  if (!s) return "-";
  return s.replace("T", " ").slice(0, 16);
}
function bookingTagType(s: number) {
  const m: Record<number, string> = { 0: "primary", 1: "success", 2: "info", 3: "danger" };
  return m[s] || "info";
}

const loading = ref(false);
const tableData = ref<VenueVO[]>([]);
const total = ref(0);
const query = reactive({
  current: 1,
  size: 20,
  name: undefined as string | undefined,
  venueType: undefined as number | undefined,
  status: undefined as number | undefined,
});

onMounted(() => fetchData());

async function fetchData() {
  loading.value = true;
  try {
    const res = await pageVenue(query);
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
  query.name = undefined;
  query.venueType = undefined;
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
  name: "",
  venueType: 1 as number,
  location: "",
  capacity: 1,
  openTime: "08:00:00",
  closeTime: "22:00:00",
  slotMinutes: 60,
  monthlyLimit: 0,
  price: 0,
  status: 1 as number,
});
const rules: FormRules = {
  name: [{ required: true, message: "请输入场地名称", trigger: "blur" }],
  venueType: [{ required: true, message: "请选择场地类型", trigger: "change" }],
  capacity: [{ required: true, message: "请输入容量", trigger: "blur" }],
  openTime: [{ required: true, message: "请选择开放时间", trigger: "change" }],
  closeTime: [{ required: true, message: "请选择关闭时间", trigger: "change" }],
  slotMinutes: [{ required: true, message: "请输入预约粒度", trigger: "blur" }],
};

function handleAdd() {
  isEdit.value = false;
  editId.value = "";
  Object.assign(form, {
    name: "",
    venueType: 1,
    location: "",
    capacity: 1,
    openTime: "08:00:00",
    closeTime: "22:00:00",
    slotMinutes: 60,
    monthlyLimit: 0,
    price: 0,
    status: 1,
  });
  drawerVisible.value = true;
}

async function handleEdit(row: VenueVO) {
  const res = await getVenue(row.id);
  const d = res.data as VenueVO;
  isEdit.value = true;
  editId.value = row.id;
  Object.assign(form, {
    name: d.name,
    venueType: d.venueType,
    location: d.location || "",
    capacity: d.capacity,
    openTime: d.openTime || "08:00:00",
    closeTime: d.closeTime || "22:00:00",
    slotMinutes: d.slotMinutes,
    monthlyLimit: d.monthlyLimit ?? 0,
    price: Number(d.price ?? 0),
    status: d.status,
  });
  drawerVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  const payload: VenueCreateRequest = {
    name: form.name,
    venueType: form.venueType,
    location: form.location || undefined,
    capacity: form.capacity,
    openTime: form.openTime,
    closeTime: form.closeTime,
    slotMinutes: form.slotMinutes,
    monthlyLimit: form.monthlyLimit,
    price: form.price,
    status: form.status,
  };
  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await updateVenue({ ...payload, id: editId.value });
      ElMessage.success("修改成功");
    } else {
      await createVenue(payload);
      ElMessage.success("创建成功");
    }
    drawerVisible.value = false;
    fetchData();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: VenueVO) {
  ElMessageBox.confirm(`确认删除场地「${row.name}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      await deleteVenue(row.id);
      ElMessage.success("已删除");
      fetchData();
    })
    .catch(() => {});
}

// ===== 预约记录 =====
const bookingVisible = ref(false);
const bookingLoading = ref(false);
const bookingData = ref<VenueBookingVO[]>([]);
const bookingTotal = ref(0);
const currentVenueName = ref("");
const bookingQuery = reactive({
  current: 1,
  size: 20,
  venueId: undefined as string | undefined,
});

function showBookings(row: VenueVO) {
  currentVenueName.value = row.name;
  bookingQuery.venueId = row.id;
  bookingQuery.current = 1;
  bookingVisible.value = true;
  fetchBookings();
}

async function fetchBookings() {
  bookingLoading.value = true;
  try {
    const res = await pageVenueBooking(bookingQuery);
    bookingData.value = (res?.data?.records || []).filter(Boolean);
    bookingTotal.value = Number(res?.data?.total || 0);
  } finally {
    bookingLoading.value = false;
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
.form-tip {
  width: 100%;
  color: #c0c4cc;
  font-size: 12px;
  line-height: 1.6;
}
</style>
