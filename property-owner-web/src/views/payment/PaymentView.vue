<template>
  <div class="page">
    <div class="page-body">
      <van-loading v-if="billsLoading" class="loading-center" />

      <van-cell-group inset title="选择要缴费的账单">
        <van-cell
          v-for="bill in bills"
          :key="bill.id"
          :title="bill.billPeriod"
          :label="bill.buildingName + ' ' + bill.roomCode"
          @click="toggleBill(bill)"
        >
          <template #icon>
            <van-checkbox
              :model-value="selectedIds.has(bill.id)"
              @click.stop
              style="margin-right: 8px"
            />
          </template>
          <template #value>
            <span class="bill-amount"
              >&yen;{{
                (
                  bill.totalAmount -
                  bill.paidAmount +
                  (bill.lateFee || 0)
                ).toFixed(2)
              }}</span
            >
          </template>
          <template #extra>
            <van-tag :type="statusTag(bill.status)" size="medium">{{
              statusName(bill.status)
            }}</van-tag>
          </template>
        </van-cell>
        <van-empty
          v-if="!billsLoading && bills.length === 0"
          description="暂无待缴费账单"
        />
      </van-cell-group>

      <div v-if="selectedIds.size > 0" class="summary-card">
        <van-cell-group inset>
          <van-cell title="选择账单" :value="selectedIds.size + ' 笔'" />
          <van-cell title="合计金额">
            <template #value>
              <span class="summary-amount"
                >&yen;{{ totalAmount.toFixed(2) }}</span
              >
            </template>
          </van-cell>
        </van-cell-group>
        <div class="submit-wrapper">
          <van-button
            type="primary"
            block
            round
            size="large"
            :loading="payLoading"
            loading-text="支付中..."
            @click="submitPay"
          >
            确认支付
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { showFailToast, showConfirmDialog } from "vant";
import { getBillPage } from "@/api/bill";
import { createPayOrder } from "@/api/payment";
import type { BillVO } from "@/api/bill";

const route = useRoute();
const router = useRouter();
const billsLoading = ref(false);
const payLoading = ref(false);
const bills = ref<BillVO[]>([]);
const selectedIds = ref(new Set<string>());
const paying = ref(false); // 标记是否已提交支付表单

const totalAmount = computed(() => {
  let total = 0;
  for (const bill of bills.value) {
    if (selectedIds.value.has(bill.id)) {
      total += bill.totalAmount - bill.paidAmount + (bill.lateFee || 0);
    }
  }
  return total;
});

const statusMap: Record<number, string> = {
  0: "未缴费",
  1: "部分缴费",
  2: "已缴清",
  3: "已作废",
  4: "已减免",
  5: "已逾期",
};
function statusName(s: number) {
  return statusMap[s] || "未知";
}
function statusTag(s: number) {
  return s === 2
    ? "success"
    : s === 5
      ? "danger"
      : s === 1
        ? "warning"
        : "primary";
}

function toggleBill(bill: BillVO) {
  if (selectedIds.value.has(bill.id)) {
    selectedIds.value.delete(bill.id);
  } else {
    selectedIds.value.add(bill.id);
  }
}

async function submitPay() {
  if (selectedIds.value.size === 0) {
    showFailToast("请选择至少一个账单");
    return;
  }
  if (selectedIds.value.size > 1) {
    showFailToast("暂仅支持单笔账单支付");
    return;
  }
  const billId = Array.from(selectedIds.value)[0];
  try {
    await showConfirmDialog({
      title: "支付确认",
      message: `确认支付 ¥${totalAmount.value.toFixed(2)} ？`,
      confirmButtonText: "确认支付",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  payLoading.value = true;
  try {
    const res = await createPayOrder({
      billId,
      paymentMethod: 1,
      payerName: "业主",
    });
    const data = res.data as any;
    if (data.payFormHtml) {
      // 标记已提交支付，用于 pageshow 回退兜底
      paying.value = true;
      sessionStorage.setItem("__paying__", "1");
      // 当前窗口跳转到支付宝收银台（同一窗口，支付完成后由 return-url 重定向回本系统）
      const container = document.createElement("div");
      container.innerHTML = data.payFormHtml;
      const form = container.querySelector("form");
      if (form) {
        // 剥离支付宝 SDK 返回表单中可能存在的反引号，避免移动端提交到损坏的 URL
        const action = (form.getAttribute("action") || "").replace(/`/g, "");
        form.setAttribute("action", action);
        document.body.appendChild(form);
        form.submit();
        return;
      }
    }
    selectedIds.value.clear();
    router.push("/records");
  } catch (e: any) {
    showFailToast(e?.msg || e?.message || "支付失败");
  } finally {
    payLoading.value = false;
  }
}

onMounted(async () => {
  billsLoading.value = true;
  try {
    const res = await getBillPage({ current: 1, size: 50 });
    const data = res.data as any;
    bills.value = (data.records || []).filter(
      (b: BillVO) => b.status !== 2 && b.status !== 3 && b.status !== 4,
    );
    const billId = route.query.billId;
    if (billId) {
      const found = bills.value.find((b) => b.id === String(billId));
      if (found) {
        selectedIds.value.add(found.id);
      }
    }
  } finally {
    billsLoading.value = false;
  }

  // 注册 pageshow 回退兜底：若用户从支付宝支付页返回本页，自动跳转至支付记录
  window.addEventListener("pageshow", handlePageShow);
});

onUnmounted(() => {
  window.removeEventListener("pageshow", handlePageShow);
});

function handlePageShow(event: PageTransitionEvent) {
  // 仅当从 bfcache 恢复（回退）且之前已提交支付时触发
  if (event.persisted && sessionStorage.getItem("__paying__") === "1") {
    sessionStorage.removeItem("__paying__");
    router.replace("/records");
  }
}
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.bill-amount {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}

.summary-card {
  margin-top: 12px;
}

.summary-amount {
  font-size: 20px;
  font-weight: bold;
  color: #e6a23c;
}

.submit-wrapper {
  padding: 16px;
}
</style>
