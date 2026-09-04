<template>
  <div class="venue-page">
    <van-nav-bar title="场地预约" left-text="返回" left-arrow @click-left="router.back()" />

    <van-tabs v-model:active="activeTab" sticky color="#3b82f6" line-width="24">
      <van-tab title="预约场地" name="book" />
      <van-tab title="我的预约" name="mine" />
    </van-tabs>

    <!-- 预约场地 -->
    <div v-show="activeTab === 'book'" class="tab-panel">
      <div v-if="venueLoading && venues.length === 0" class="skeleton-list">
        <van-skeleton v-for="i in 3" :key="i" title :row="2" style="margin-bottom: 12px" />
      </div>

      <div v-for="v in venues" :key="v.id" class="venue-card" @click="openBooking(v)">
        <div class="venue-icon">
          <van-icon :name="venueIcon(v.venueType)" size="24" color="#3b82f6" />
        </div>
        <div class="venue-info">
          <div class="venue-name">{{ v.name }}</div>
          <div class="venue-meta">
            <van-tag plain type="primary">{{ v.venueTypeName }}</van-tag>
            <span>{{ fmtTime(v.openTime) }} ~ {{ fmtTime(v.closeTime) }}</span>
          </div>
        </div>
        <div class="venue-price">
          <span v-if="Number(v.price ?? 0) === 0" class="free">免费</span>
          <span v-else class="price">¥{{ Number(v.price).toFixed(2) }}</span>
        </div>
      </div>

      <van-empty v-if="!venueLoading && venues.length === 0" description="暂无可用场地" />
    </div>

    <!-- 我的预约 -->
    <div v-show="activeTab === 'mine'" class="tab-panel">
      <van-list v-model:loading="bookingLoading" :finished="bookingFinished" finished-text="没有更多预约了" @load="loadBookings">
        <div v-for="b in bookings" :key="b.id" class="booking-card">
          <div class="booking-head">
            <span class="booking-name">{{ b.venueName }}</span>
            <van-tag :color="bookingStatusColor(b.status)" text-color="#fff" round>{{ b.statusName }}</van-tag>
          </div>
          <div class="booking-meta">
            <van-icon name="calendar-o" />
            <span>{{ b.bookingDate }} {{ fmtTime(b.startTime) }} ~ {{ fmtTime(b.endTime) }}</span>
          </div>
          <div class="booking-actions">
            <van-button v-if="b.status === 0" size="small" plain type="danger" round @click="cancelBooking(b)">
              取消预约
            </van-button>
          </div>
        </div>
      </van-list>
      <van-empty v-if="!bookingLoading && bookings.length === 0" description="暂无预约" />
    </div>

    <!-- 预约弹层 -->
    <van-popup v-model:show="bookingVisible" position="bottom" round :style="{ maxHeight: '80%' }">
      <div class="booking-panel">
        <div class="panel-title">{{ currentVenue?.name }}</div>

        <div class="date-label">选择日期</div>
        <div class="date-strip">
          <div
            v-for="d in dateList"
            :key="d.value"
            class="date-item"
            :class="{ active: selectedDate === d.value }"
            @click="selectDate(d.value)"
          >
            <span class="date-week">{{ d.week }}</span>
            <span class="date-day">{{ d.day }}</span>
          </div>
        </div>

        <div class="date-label">选择时段</div>
        <div v-if="slotLoading" class="slot-loading">
          <van-skeleton title :row="2" />
        </div>
        <div v-else class="slot-grid">
          <div
            v-for="s in slots"
            :key="s.start"
            class="slot-item"
            :class="{ active: selectedSlot === s.start, disabled: s.occupied }"
            @click="selectSlot(s)"
          >
            {{ fmtTime(s.start) }}
          </div>
        </div>
        <van-empty v-if="!slotLoading && slots.length === 0" description="当日无可预约时段" />

        <div class="panel-footer">
          <van-button block round plain @click="bookingVisible = false">取消</van-button>
          <van-button type="primary" block round :loading="submitLoading" @click="submitBooking">确认预约</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast, showConfirmDialog } from 'vant'
import {
  venueList,
  venueSlots,
  bookVenue,
  myVenueBookings,
  cancelVenueBooking,
  BOOKING_STATUS_MAP,
} from '@/api/service'
import type { VenueVO, VenueBookingVO } from '@/api/service'

const router = useRouter()

function fmtTime(s?: string) {
  if (!s) return '-'
  return s.slice(0, 5)
}
function venueIcon(t: number) {
  const m: Record<number, string> = { 1: 'fire-o', 2: 'friends-o', 3: 'chat-o', 4: 'flower-o', 5: 'shop-o' }
  return m[t] || 'shop-o'
}
function bookingStatusColor(s: number) {
  const m: Record<number, string> = { 0: '#3b82f6', 1: '#22c55e', 2: '#94a3b8', 3: '#ef4444' }
  return m[s] || '#94a3b8'
}
function pad(n: number) {
  return String(n).padStart(2, '0')
}
function toDateStr(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function toMinutes(t: string) {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + (m || 0)
}
function toTime(min: number) {
  return `${pad(Math.floor(min / 60))}:${pad(min % 60)}:00`
}

// 预约场地
const activeTab = ref('book')
const venues = ref<VenueVO[]>([])
const venueLoading = ref(false)

async function loadVenues() {
  venueLoading.value = true
  try {
    const res = await venueList()
    venues.value = (res?.data || []).filter(Boolean)
  } finally {
    venueLoading.value = false
  }
}

// 我的预约
const bookings = ref<VenueBookingVO[]>([])
const bookingLoading = ref(false)
const bookingFinished = ref(false)
const bookingPage = ref(1)

async function loadBookings() {
  try {
    const res = await myVenueBookings({ current: bookingPage.value, size: 10 })
    const records = (res?.data?.records || []).filter(Boolean)
    bookings.value.push(...records)
    bookingFinished.value = bookings.value.length >= Number(res?.data?.total || 0)
    bookingPage.value += 1
  } catch {
    bookingFinished.value = true
  } finally {
    bookingLoading.value = false
  }
}

function cancelBooking(b: VenueBookingVO) {
  showConfirmDialog({ title: '提示', message: `确认取消「${b.venueName}」的预约？` })
    .then(async () => {
      await cancelVenueBooking(b.id)
      showSuccessToast('已取消')
      bookings.value = []
      bookingPage.value = 1
      bookingFinished.value = false
      bookingLoading.value = true
      loadBookings()
    })
    .catch(() => {})
}

// 预约弹层
const bookingVisible = ref(false)
const currentVenue = ref<VenueVO>()
const slotLoading = ref(false)
const submitLoading = ref(false)
const dateList = ref<Array<{ value: string; week: string; day: number }>>([])
const selectedDate = ref('')
const slots = ref<Array<{ start: string; end: string; occupied: boolean }>>([])
const selectedSlot = ref('')

function buildDateList() {
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  const list: Array<{ value: string; week: string; day: number }> = []
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    list.push({ value: toDateStr(d), week: '周' + weeks[d.getDay()], day: d.getDate() })
  }
  dateList.value = list
  selectedDate.value = list[0].value
}

function openBooking(v: VenueVO) {
  currentVenue.value = v
  selectedSlot.value = ''
  buildDateList()
  bookingVisible.value = true
  loadSlots()
}

async function loadSlots() {
  if (!currentVenue.value) return
  slotLoading.value = true
  selectedSlot.value = ''
  try {
    const res = await venueSlots(currentVenue.value.id, selectedDate.value)
    const data = res.data
    const open = data.openTime || currentVenue.value.openTime
    const close = data.closeTime || currentVenue.value.closeTime
    const step = data.slotMinutes || currentVenue.value.slotMinutes || 60
    const occupied = data.occupied || []
    const list: Array<{ start: string; end: string; occupied: boolean }> = []
    const openMin = toMinutes(open)
    const closeMin = toMinutes(close)
    for (let start = openMin; start + step <= closeMin; start += step) {
      const s = { start: toTime(start), end: toTime(start + step), occupied: false }
      s.occupied = occupied.some(
        (o: { startTime: string; endTime: string }) =>
          toMinutes(s.start) < toMinutes(o.endTime) && toMinutes(s.end) > toMinutes(o.startTime),
      )
      list.push(s)
    }
    slots.value = list
  } finally {
    slotLoading.value = false
  }
}

function selectDate(date: string) {
  selectedDate.value = date
  loadSlots()
}

function selectSlot(s: { start: string; occupied: boolean }) {
  if (s.occupied) return
  selectedSlot.value = s.start
}

async function submitBooking() {
  if (!currentVenue.value) return
  if (!selectedSlot.value) {
    showToast('请选择时段')
    return
  }
  const slot = slots.value.find((s) => s.start === selectedSlot.value)
  if (!slot) return
  submitLoading.value = true
  try {
    await bookVenue(currentVenue.value.id, {
      bookingDate: selectedDate.value,
      startTime: slot.start,
      endTime: slot.end,
    })
    showSuccessToast('预约成功')
    bookingVisible.value = false
    bookings.value = []
    bookingPage.value = 1
    bookingFinished.value = false
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadVenues()
  if (activeTab.value === 'mine') {
    loadBookings()
  }
})
</script>

<style scoped>
.venue-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.tab-panel {
  padding: 12px;
}
.skeleton-list {
  padding: 4px;
}
.venue-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  transition: transform 0.2s;
  cursor: pointer;
}
.venue-card:active {
  transform: scale(0.99);
}
.venue-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.venue-info {
  flex: 1;
  min-width: 0;
}
.venue-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}
.venue-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}
.venue-price {
  flex-shrink: 0;
}
.free {
  color: #22c55e;
  font-weight: 600;
}
.price {
  color: #f59e0b;
  font-weight: 600;
}

.booking-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
}
.booking-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.booking-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}
.booking-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 10px;
}
.booking-actions {
  display: flex;
  justify-content: flex-end;
}

.booking-panel {
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
}
.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
  text-align: center;
  margin-bottom: 16px;
}
.date-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 10px;
}
.date-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
  scrollbar-width: none;
}
.date-strip::-webkit-scrollbar {
  display: none;
}
.date-item {
  flex-shrink: 0;
  width: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.date-item.active {
  background: #eff6ff;
  border-color: #3b82f6;
}
.date-week {
  font-size: 11px;
  color: #94a3b8;
}
.date-day {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
.date-item.active .date-day,
.date-item.active .date-week {
  color: #3b82f6;
}
.slot-loading {
  padding: 8px 0;
}
.slot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.slot-item {
  text-align: center;
  padding: 10px 0;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.slot-item.active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}
.slot-item.disabled {
  background: #f1f5f9;
  color: #cbd5e1;
  cursor: not-allowed;
  text-decoration: line-through;
}
.panel-footer {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
</style>
