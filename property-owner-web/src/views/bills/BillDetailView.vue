<template>
  <div class="page">
    <van-nav-bar title="账单明细" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body" v-if="detail">
      <van-loading v-if="loading" class="loading-center" />

      <van-cell-group inset title="基本信息">
        <van-cell title="账单周期">
          <template #value>
            <span class="bill-period">{{ detail.billPeriod }}</span>
            <van-tag :type="statusTag(detail.status)" size="medium" class="status-tag">
              {{ statusName(detail.status) }}
            </van-tag>
          </template>
        </van-cell>
        <van-cell title="房屋" :value="detail.buildingName + ' ' + detail.roomCode" />
        <van-cell title="账单编号" :value="detail.billNo" />
        <van-cell title="出账日期" :value="detail.billDate" />
        <van-cell title="缴费截止" :value="detail.dueDate" />
      </van-cell-group>

      <van-cell-group inset title="费用明细" v-if="detail.items && detail.items.length > 0">
        <van-cell
          v-for="item in detail.items"
          :key="item.id"
          :title="item.feeItemName"
          :label="item.calcBase + ' × ' + item.unitPrice + ' × ' + item.quantity"
        >
          <template #value>
            <span class="item-amount">&yen;{{ item.amount }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <van-cell-group inset title="金额汇总">
        <van-cell title="应缴金额" :value="'¥' + detail.totalAmount" />
        <van-cell v-if="detail.paidAmount > 0" title="已缴金额" :value="'¥' + detail.paidAmount" />
        <van-cell v-if="detail.lateFee > 0" title="逾期罚息" :value="'¥' + detail.lateFee" />
        <van-cell title="待缴金额">
          <template #value>
            <span class="due-amount-value">&yen;{{ dueAmount }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <div class="pay-btn-wrapper" v-if="detail.status !== 2">
        <van-button type="primary" block round size="large" @click="goPay">立即缴费</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { getBillDetail } from '@/api/bill'
import type { BillDetailVO } from '@/api/bill'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<BillDetailVO>()

const dueAmount = computed(() => {
  if (!detail.value) return 0
  const paid = detail.value.paidAmount || 0
  const late = detail.value.lateFee || 0
  return (detail.value.totalAmount - paid + late).toFixed(2)
})

function goBack() { router.back() }
function goPay() {
  router.push({ path: '/payment', query: { billId: detail.value?.id } })
}

const statusMap: Record<number, string> = { 0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期' }
function statusName(s: number) { return statusMap[s] || '未知' }
function statusTag(s: number) { return s === 2 ? 'success' : s === 5 ? 'danger' : s === 1 ? 'warning' : 'primary' }

onMounted(async () => {
  loading.value = true
  try {
    const id = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
    const res = await getBillDetail(id)
    detail.value = res.data as BillDetailVO
  } catch (e: any) {
    showFailToast(e?.msg || e?.message || '账单不存在或已被删除')
    setTimeout(() => router.back(), 1500)
  } finally { loading.value = false }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
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

.bill-period {
  font-weight: bold;
}

.status-tag {
  margin-left: 8px;
}

.item-amount {
  font-weight: bold;
  color: #e6a23c;
}

.due-amount-value {
  font-size: 20px;
  font-weight: bold;
  color: #e6a23c;
}

.pay-btn-wrapper {
  padding: 24px 16px;
}
</style>
