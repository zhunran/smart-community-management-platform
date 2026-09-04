<template>
  <div class="visitor-page">
    <van-nav-bar title="访客邀请" left-text="返回" left-arrow @click-left="router.back()" />

    <van-tabs v-model:active="activeTab" sticky color="#3b82f6" line-width="24">
      <van-tab title="邀请访客" name="invite" />
      <van-tab title="我的通行码" name="mine" />
    </van-tabs>

    <!-- 邀请访客 -->
    <div v-show="activeTab === 'invite'" class="tab-panel">
      <div class="form-card">
        <van-field v-model="form.visitorName" label="访客姓名" placeholder="请输入访客姓名" maxlength="50" />
        <van-field v-model="form.visitorPhone" label="手机号" placeholder="选填" maxlength="20" type="tel" />
        <van-field v-model="form.plateNo" label="车牌号" placeholder="选填" maxlength="20" />

        <van-field
          v-model="form.validFrom"
          label="生效时间"
          placeholder="请选择生效时间"
          readonly
          is-link
          @click="openPicker('from')"
        />
        <van-field
          v-model="form.validUntil"
          label="失效时间"
          placeholder="请选择失效时间"
          readonly
          is-link
          @click="openPicker('until')"
        />

        <div class="max-use-row">
          <span class="max-use-label">可用次数</span>
          <div class="max-use-right">
            <van-stepper v-model="form.maxUse" :min="0" :max="99" />
            <span class="max-use-tip">0 表示不限</span>
          </div>
        </div>
      </div>

      <div class="submit-wrap">
        <van-button type="primary" block round :loading="submitting" @click="submit">生成通行码</van-button>
      </div>
    </div>

    <!-- 我的通行码 -->
    <div v-show="activeTab === 'mine'" class="tab-panel">
      <van-list v-model:loading="listLoading" :finished="listFinished" finished-text="没有更多了" @load="loadList">
        <div v-for="p in list" :key="p.id" class="pass-card">
          <div class="pass-head">
            <span class="pass-code">{{ p.passCode }}</span>
            <van-tag :color="statusColor(p.status)" text-color="#fff" round>{{ p.statusName }}</van-tag>
          </div>
          <div class="pass-meta">访客：{{ p.visitorName }}</div>
          <div class="pass-meta">有效期：{{ p.validFrom }} ~ {{ p.validUntil }}</div>
          <div class="pass-meta">已用：{{ p.usedCount }} / {{ p.maxUse === 0 ? '不限' : p.maxUse }}</div>
          <div class="pass-actions">
            <van-button v-if="p.status === 0" size="small" plain type="danger" round @click="revoke(p)">
              撤销
            </van-button>
          </div>
        </div>
      </van-list>
      <van-empty v-if="!listLoading && list.length === 0" description="暂无通行码" />
    </div>

    <!-- 时间选择 -->
    <van-popup v-model:show="pickerVisible" position="bottom" round>
      <van-date-picker
        v-model="pickerValue"
        type="datetime"
        title="选择时间"
        :min-date="minDate"
        @confirm="onPickerConfirm"
        @cancel="pickerVisible = false"
      />
    </van-popup>

    <!-- 二维码弹层 -->
    <van-popup v-model:show="qrVisible" round :style="{ width: '82%' }">
      <div class="qr-panel">
        <div class="qr-title">访客通行码</div>
        <div v-if="qrDataUrl" class="qr-box">
          <img :src="qrDataUrl" alt="二维码" class="qr-img" />
        </div>
        <div class="qr-code">{{ currentPass?.passCode }}</div>
        <div class="qr-info">
          <div>访客：{{ currentPass?.visitorName }}</div>
          <div>有效期至：{{ currentPass?.validUntil }}</div>
        </div>
        <div class="qr-actions">
          <van-button size="small" plain round icon="share-o" @click="sharePass">分享</van-button>
          <van-button size="small" plain round @click="copyCode">复制通行码</van-button>
          <van-button size="small" type="primary" round @click="qrVisible = false">完成</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast, showConfirmDialog } from 'vant'
import QRCode from 'qrcode'
import { createVisitorPass, myVisitorPasses, revokeVisitorPass } from '@/api/service'
import type { VisitorPassVO, VisitorPassCreateRequest } from '@/api/service'

const router = useRouter()

function statusColor(s: number) {
  const m: Record<number, string> = { 0: '#22c55e', 1: '#f59e0b', 2: '#94a3b8', 3: '#ef4444' }
  return m[s] || '#94a3b8'
}

const activeTab = ref('invite')
const submitting = ref(false)

const form = reactive({
  visitorName: '',
  visitorPhone: '',
  plateNo: '',
  validFrom: '',
  validUntil: '',
  maxUse: 1,
})

// 时间选择
const pickerVisible = ref(false)
const pickerTarget = ref<'from' | 'until'>('from')
const pickerValue = ref<string[]>([])
const minDate = new Date()

function openPicker(target: 'from' | 'until') {
  pickerTarget.value = target
  const base = target === 'from' ? form.validFrom : form.validUntil
  pickerValue.value = base ? base.replace(' ', '-').split(':').slice(0, 5).join('-').split('-').map((s) => String(Number(s))) : []
  pickerVisible.value = true
}

function onPickerConfirm({ selectedValues }: { selectedValues: string[] }) {
  const [y, mo, d, h, mi] = selectedValues
  const val = `${y}-${mo}-${d} ${h}:${mi}:00`
  if (pickerTarget.value === 'from') form.validFrom = val
  else form.validUntil = val
  pickerVisible.value = false
}

async function submit() {
  if (!form.visitorName.trim()) {
    showToast('请输入访客姓名')
    return
  }
  if (!form.validFrom || !form.validUntil) {
    showToast('请选择生效与失效时间')
    return
  }
  if (form.validUntil <= form.validFrom) {
    showToast('失效时间需晚于生效时间')
    return
  }
  submitting.value = true
  try {
    const payload: VisitorPassCreateRequest = {
      visitorName: form.visitorName.trim(),
      visitorPhone: form.visitorPhone.trim() || undefined,
      plateNo: form.plateNo.trim() || undefined,
      validFrom: form.validFrom,
      validUntil: form.validUntil,
      maxUse: form.maxUse,
    }
    const res = await createVisitorPass(payload)
    const pass = res.data as VisitorPassVO
    showSuccessToast('生成成功')
    showQr(pass)
  } finally {
    submitting.value = false
  }
}

// 我的通行码
const list = ref<VisitorPassVO[]>([])
const listLoading = ref(false)
const listFinished = ref(false)
const listPage = ref(1)

async function loadList() {
  try {
    const res = await myVisitorPasses({ current: listPage.value, size: 10 })
    const records = (res?.data?.records || []).filter(Boolean)
    list.value.push(...records)
    listFinished.value = list.value.length >= Number(res?.data?.total || 0)
    listPage.value += 1
  } catch {
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

function revoke(p: VisitorPassVO) {
  showConfirmDialog({ title: '提示', message: `确认撤销通行码 ${p.passCode}？` })
    .then(async () => {
      await revokeVisitorPass(p.id)
      showSuccessToast('已撤销')
      list.value = []
      listPage.value = 1
      listFinished.value = false
      listLoading.value = true
      loadList()
    })
    .catch(() => {})
}

// 二维码
const qrVisible = ref(false)
const qrDataUrl = ref('')
const currentPass = ref<VisitorPassVO>()

async function showQr(pass: VisitorPassVO) {
  currentPass.value = pass
  qrVisible.value = true
  try {
    qrDataUrl.value = await QRCode.toDataURL(pass.passCode, { width: 220, margin: 1 })
  } catch {
    qrDataUrl.value = ''
  }
}

function copyCode() {
  if (!currentPass.value) return
  navigator.clipboard?.writeText(currentPass.value.passCode).then(() => showSuccessToast('已复制'))
}

async function sharePass() {
  if (!currentPass.value) return
  const text = `访客通行码：${currentPass.value.passCode}（${currentPass.value.visitorName}，有效期至 ${currentPass.value.validUntil}）`
  if (navigator.share) {
    try {
      await navigator.share({ title: '访客通行码', text })
    } catch {
      /* 用户取消 */
    }
  } else {
    copyCode()
  }
}

onMounted(() => {
  if (activeTab.value === 'mine') loadList()
})
</script>

<style scoped>
.visitor-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.tab-panel {
  padding: 12px;
}
.form-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  padding: 6px 0;
}
.max-use-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}
.max-use-label {
  font-size: 14px;
  color: #323233;
}
.max-use-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.max-use-tip {
  font-size: 12px;
  color: #94a3b8;
}
.submit-wrap {
  padding: 8px 4px;
}

.pass-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
}
.pass-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.pass-code {
  font-size: 20px;
  font-weight: 700;
  color: #3b82f6;
  letter-spacing: 2px;
}
.pass-meta {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}
.pass-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.qr-panel {
  padding: 24px 20px;
  text-align: center;
}
.qr-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}
.qr-box {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}
.qr-img {
  width: 220px;
  height: 220px;
}
.qr-code {
  font-size: 22px;
  font-weight: 700;
  color: #3b82f6;
  letter-spacing: 3px;
  margin-bottom: 12px;
}
.qr-info {
  font-size: 13px;
  color: #64748b;
  line-height: 1.8;
  margin-bottom: 18px;
}
.qr-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
