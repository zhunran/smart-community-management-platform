<template>
  <div class="page">
    <van-nav-bar title="支付记录" left-text="返回" left-arrow @click-left="goBack" />

    <div class="page-body">
      <van-loading v-if="loading" class="loading-center" />
      <template v-else>
        <van-cell
          v-for="r in records"
          :key="r.id"
          :title="'支付单号：' + r.paymentNo"
          :label="r.billPeriod + ' ' + (r.billNo || '')"
          is-link
          @click="showDetail(r)"
        >
          <template #value>
            <div class="record-amount">&yen;{{ r.paymentAmount }}</div>
          </template>
          <template #extra>
            <van-tag :type="payStatusTag(r.paymentStatus)" size="medium">
              {{ r.paymentStatusName }}
            </van-tag>
          </template>
        </van-cell>
        <van-empty v-if="records.length === 0" description="暂无支付记录" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPaymentPage } from '@/api/payment'
import type { PaymentOrderVO } from '@/api/payment'
import { showDialog } from 'vant'

const router = useRouter()
const loading = ref(false)
const records = ref<PaymentOrderVO[]>([])
function goBack() { router.back() }

function payStatusTag(s: number) { return s === 2 ? 'success' : s === 3 ? 'danger' : s === 0 ? 'warning' : 'primary' }

function showDetail(r: PaymentOrderVO) {
  showDialog({
    title: '支付详情',
    message:
      `支付单号：${r.paymentNo}<br>` +
      `金额：¥${r.paymentAmount}<br>` +
      `方式：${r.paymentMethodName}<br>` +
      `流水号：${r.transactionId || '-'}<br>` +
      `状态：${r.paymentStatusName}<br>` +
      `时间：${r.paymentTime || r.createTime}`,
    allowHtml: true,
    confirmButtonText: '知道了',
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getPaymentPage({ current: 1, size: 50 })
    const data = res.data as any
    records.value = data.records || []
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

.record-amount {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}
</style>
