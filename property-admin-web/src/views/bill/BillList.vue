<template>
  <div class="bill-list">
    <el-card>
      <template #header
        ><div class="card-header">
          <span>账单管理</span>
          <div>
            <el-button type="success" @click="handleGenerate"
              >生成账单</el-button
            >
          </div>
        </div></template
      >
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="账期"
          ><el-input
            v-model="query.billPeriod"
            placeholder="如2026-06"
            style="width: 110px"
            clearable
        /></el-form-item>
        <el-form-item label="楼栋"
          ><el-select
            v-model="query.buildingId"
            clearable
            filterable
            style="width: 140px"
            ><el-option
              v-for="b in buildings"
              :key="b.id"
              :label="b.buildingName"
              :value="b.id" /></el-select
        ></el-form-item>
        <el-form-item label="房号"
          ><el-input v-model="query.roomCode" clearable style="width: 100px"
        /></el-form-item>
        <el-form-item label="状态"
          ><el-select v-model="query.status" clearable style="width: 110px"
            ><el-option
              v-for="(l, k) in STATUS_MAP"
              :key="k"
              :label="l"
              :value="Number(k)" /></el-select
        ></el-form-item>
        <el-form-item
          ><el-button type="primary" @click="search">查询</el-button
          ><el-button @click="reset">重置</el-button></el-form-item
        >
      </el-form>
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="billNo" label="账单编号" width="160" />
        <el-table-column prop="buildingName" label="楼栋" width="100" />
        <el-table-column prop="roomCode" label="房号" width="90" />
        <el-table-column prop="ownerName" label="业主" width="90" />
        <el-table-column
          prop="billPeriod"
          label="账期"
          width="80"
          align="center"
        />
        <el-table-column
          prop="totalAmount"
          label="总金额"
          width="100"
          align="right"
          ><template #default="{ row }"
            >¥{{ row.totalAmount }}</template
          ></el-table-column
        >
        <el-table-column prop="paidAmount" label="已交" width="90" align="right"
          ><template #default="{ row }"
            >¥{{ row.paidAmount }}</template
          ></el-table-column
        >
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{
              STATUS_MAP[row.status]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">明细</el-button>
            <el-button
              size="small"
              type="success"
              :disabled="row.status === 2 || row.status === 3"
              @click="handlePay(row)"
              >缴费</el-button
            >
            <el-button
              size="small"
              type="danger"
              :disabled="row.status === 3"
              @click="handleVoid(row)"
              >作废</el-button
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
    <!-- 生成账单 -->
    <el-dialog v-model="genVisible" title="生成账单" width="420px">
      <el-form
        ref="genFormRef"
        :model="genForm"
        :rules="genRules"
        label-width="100px"
      >
        <el-form-item label="账期" prop="billPeriod">
          <el-input
            v-model="genForm.billPeriod"
            placeholder="如 2026-06"
            maxlength="7"
          />
        </el-form-item>
        <el-form-item label="指定业主">
          <el-select
            v-model="genForm.ownerId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="留空则生成全部"
            :loading="ownerLoading"
            :remote-method="fetchOwners"
            style="width: 100%"
          >
            <el-option
              v-for="o in ownerList"
              :key="o.id"
              :label="`${o.ownerName} (${o.phone})`"
              :value="Number(o.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="genForm.dueDateStr"
            type="date"
            placeholder="默认月末"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" :loading="genLoading" @click="handleGenSubmit"
          >生成</el-button
        >
      </template>
    </el-dialog>
    <!-- 账单明细 -->
    <el-dialog v-model="detailVisible" title="账单明细" width="700px">
      <template v-if="detail">
        <div class="detail-header">
          <span>账单编号：{{ detail.billNo }}</span
          ><span>业主：{{ detail.ownerName }}</span
          ><span>房号：{{ detail.roomCode }}</span>
          <span>账期：{{ detail.billPeriod }}</span
          ><span>截止日：{{ detail.dueDate }}</span>
          <el-tag :type="tagType(detail.status)" size="small">{{
            STATUS_MAP[detail.status]
          }}</el-tag>
        </div>
        <el-table
          :data="detail.items"
          stripe
          border
          size="small"
          style="width: 100%; margin-top: 12px"
        >
          <el-table-column prop="feeItemName" label="费用项" width="120" />
          <el-table-column
            prop="calcBase"
            label="基数"
            width="80"
            align="right"
          />
          <el-table-column
            prop="unitPrice"
            label="单价"
            width="80"
            align="right"
            ><template #default="{ row }"
              >¥{{ row.unitPrice }}</template
            ></el-table-column
          >
          <el-table-column
            prop="quantity"
            label="数量"
            width="60"
            align="center"
          />
          <el-table-column prop="amount" label="金额" width="90" align="right"
            ><template #default="{ row }"
              >¥{{ row.amount }}</template
            ></el-table-column
          >
          <el-table-column
            prop="paidAmount"
            label="已交"
            width="80"
            align="right"
            ><template #default="{ row }"
              >¥{{ row.paidAmount }}</template
            ></el-table-column
          >
          <el-table-column
            prop="remark"
            label="备注"
            min-width="100"
            show-overflow-tooltip
          />
        </el-table>
        <div class="detail-total">
          合计：¥{{ detail.totalAmount }} | 已交：¥{{ detail.paidAmount }} |
          滞纳金：¥{{ detail.lateFee }}
        </div>
      </template>
    </el-dialog>
    <!-- 手动缴费 -->
    <el-dialog v-model="payVisible" title="手动缴费" width="450px">
      <el-form
        ref="payFormRef"
        :model="payForm"
        :rules="payRules"
        label-width="100px"
      >
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-radio-group v-model="payForm.paymentMethod"
            ><el-radio :value="4">现金</el-radio
            ><el-radio :value="5">转账</el-radio></el-radio-group
          >
        </el-form-item>
        <el-form-item label="付款人"
          ><el-input v-model="payForm.payerName" maxlength="50"
        /></el-form-item>
        <el-form-item label="备注"
          ><el-input v-model="payForm.remark" maxlength="200"
        /></el-form-item>
      </el-form>
      <template #footer
        ><el-button @click="payVisible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="payLoading"
          @click="handlePaySubmit"
          >确定缴费</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  pageBill,
  getBillDetail,
  generateBill,
  manualPayment,
  BILL_STATUS_MAP,
} from "@/api/bill";
import type { BillVO, BillDetailVO, BillGenerateRequest } from "@/api/bill";
import { listAllBuildings } from "@/api/building";
import type { BuildingVO } from "@/api/building";
import { pageOwner } from "@/api/owner";
import type { OwnerVO } from "@/api/owner";
const STATUS_MAP = BILL_STATUS_MAP;
function tagType(s: number) {
  return s === 2
    ? "success"
    : s === 0 || s === 1
      ? "warning"
      : s === 5
        ? "danger"
        : "info";
}
const loading = ref(false);
const tableData = ref<BillVO[]>([]);
const total = ref(0);
const buildings = ref<BuildingVO[]>([]);
const query = reactive({
  current: 1,
  size: 20,
  billPeriod: "",
  buildingId: undefined as number | undefined,
  roomCode: "",
  status: undefined as number | undefined,
});
onMounted(async () => {
  const res = await listAllBuildings();
  buildings.value = res.data;
  fetchData();
});
async function fetchData() {
  loading.value = true;
  try {
    const res = await pageBill(query);
    tableData.value = res.data.records;
    total.value = Number(res.data.total);
  } finally {
    loading.value = false;
  }
}
function search() {
  query.current = 1;
  fetchData();
}
function reset() {
  query.billPeriod = "";
  query.buildingId = undefined;
  query.roomCode = "";
  query.status = undefined;
  search();
}
// generate
const genVisible = ref(false);
const genLoading = ref(false);
const genFormRef = ref<FormInstance>();
const genForm = reactive({
  billPeriod: "",
  dueDateStr: "",
  ownerId: undefined as number | undefined,
});
const genRules: FormRules = {
  billPeriod: [{ required: true, message: "请输入账期", trigger: "blur" }],
};
const ownerList = ref<OwnerVO[]>([]);
const ownerLoading = ref(false);
function handleGenerate() {
  genForm.billPeriod = "";
  genForm.dueDateStr = "";
  genForm.ownerId = undefined;
  genVisible.value = true;
  fetchOwners("");
}
async function fetchOwners(keyword: string) {
  ownerLoading.value = true;
  try {
    const res = await pageOwner({
      ownerName: keyword || undefined,
      current: 1,
      size: 100,
    });
    ownerList.value = (res.data.records as OwnerVO[]) || [];
  } finally {
    ownerLoading.value = false;
  }
}
async function handleGenSubmit() {
  const valid = await genFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  genLoading.value = true;
  try {
    const data: BillGenerateRequest = { billPeriod: genForm.billPeriod };
    if (genForm.ownerId) data.ownerId = genForm.ownerId;
    if (genForm.dueDateStr) data.dueDate = genForm.dueDateStr;
    const res = await generateBill(data);
    ElMessage.success(`生成完成，共${res.data}笔`);
    genVisible.value = false;
    fetchData();
  } finally {
    genLoading.value = false;
  }
}
// detail
const detailVisible = ref(false);
const detail = ref<BillDetailVO | null>(null);
async function handleDetail(row: BillVO) {
  detailVisible.value = true;
  const res = await getBillDetail(row.id);
  detail.value = res.data;
}
// pay
const payVisible = ref(false);
const payLoading = ref(false);
const payFormRef = ref<FormInstance>();
const payBillId = ref(0);
const payForm = reactive({ paymentMethod: 4, payerName: "", remark: "" });
const payRules: FormRules = {
  paymentMethod: [
    { required: true, message: "请选择支付方式", trigger: "change" },
  ],
};
function handlePay(row: BillVO) {
  payBillId.value = row.id;
  payForm.paymentMethod = 4;
  payForm.payerName = "";
  payForm.remark = "";
  payVisible.value = true;
}
async function handlePaySubmit() {
  const valid = await payFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  payLoading.value = true;
  try {
    await manualPayment({ billId: payBillId.value, ...payForm });
    ElMessage.success("缴费成功");
    payVisible.value = false;
    fetchData();
  } finally {
    payLoading.value = false;
  }
}
// void
function handleVoid(row: BillVO) {
  ElMessageBox.confirm(`确认作废账单「${row.billNo}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      await manualPayment({
        billId: row.id,
        paymentMethod: 4,
        remark: "已作废",
      });
      ElMessage.success("已作废");
      fetchData();
    })
    .catch(() => {});
}
</script>
<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.search-form {
  margin-bottom: 0;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.detail-header {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
  font-size: 13px;
}
.detail-total {
  margin-top: 12px;
  font-weight: bold;
  font-size: 14px;
  color: #e74c3c;
}
</style>
