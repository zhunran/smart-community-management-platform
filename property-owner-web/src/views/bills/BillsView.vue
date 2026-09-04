<template>
  <div class="page">
    <van-tabs v-model:active="activeTab" sticky @change="loadBills" color="#3b82f6">
      <van-tab title="未缴费" name="unpaid" />
      <van-tab title="已缴清" name="paid" />
      <van-tab title="历史账单" name="all" />
    </van-tabs>

    <div class="page-body">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div v-for="i in 5" :key="i" class="skeleton-card">
          <van-skeleton title :row="2" />
        </div>
      </template>

      <!-- 账单列表 -->
      <template v-else>
        <van-cell
          v-for="bill in bills"
          :key="bill.id"
          :title="bill.billPeriod"
          :label="bill.buildingName + ' ' + bill.roomCode"
          is-link
          @click="goDetail(bill.id)"
        >
          <template #value>
            <div class="bill-amount">
              <span class="amount">&yen;{{ bill.totalAmount }}</span>
              <span v-if="bill.paidAmount > 0" class="amount-paid">已缴：&yen;{{ bill.paidAmount }}</span>
            </div>
          </template>
          <template #extra>
            <van-tag :type="statusTag(bill.status)" size="medium">
              {{ statusName(bill.status) }}
            </van-tag>
          </template>
        </van-cell>

        <van-empty v-if="bills.length === 0" description="暂无账单">
          <template #image>
            <van-icon name="notes-o" size="64" color="#cbd5e1" />
          </template>
        </van-empty>
      </template>

      <van-pagination
        v-if="total > size"
        v-model="current"
        :total-items="total"
        :page-size="size"
        mode="simple"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBillPage } from '@/api/bill'
import type { BillVO } from '@/api/bill'

const router = useRouter()
const activeTab = ref('unpaid')
const bills = ref<BillVO[]>([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)

function goDetail(id: string) { router.push(`/bills/${id}`) }

const statusMap: Record<number, string> = { 0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免', 5: '已逾期' }
function statusName(s: number) { return statusMap[s] || '未知' }
function statusTag(s: number) { return s === 2 ? 'success' : s === 5 ? 'danger' : s === 1 ? 'warning' : 'primary' }

function getStatusFilter(): number | undefined {
  if (activeTab.value === 'paid') return 2
  return undefined
}

async function loadBills() {
  loading.value = true
  current.value = 1
  try {
    const res = await getBillPage({ current: current.value, size: size.value, status: getStatusFilter() })
    const data = res.data as any
    bills.value = data.records || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function onPageChange(p: number) {
  current.value = p
  loading.value = true
  try {
    const res = await getBillPage({ current: current.value, size: size.value, status: getStatusFilter() })
    const data = res.data as any
    bills.value = data.records || []
  } finally { loading.value = false }
}

onMounted(loadBills)
</script>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f7fa;
}

.page-body {
  padding: 12px 0;
}

/* 骨架屏 */
.skeleton-card {
  padding: 12px 16px;
  background: #fff;
  margin-bottom: 1px;
}

/* 金额 */
.bill-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.amount {
  font-size: 16px;
  font-weight: 600;
  color: #f59e0b;
}

.amount-paid {
  font-size: 12px;
  color: #22c55e;
}
</style>