import request from '@/utils/request'

// ==================== 枚举映射 ====================

export const REPAIR_STATUS_MAP: Record<number, string> = {
  0: '待审核',
  1: '待派单',
  2: '已派单',
  3: '维修中',
  4: '已完成',
  5: '已评价',
  6: '已驳回',
  7: '已取消',
}

export const REPAIR_CATEGORY_MAP: Record<number, string> = {
  1: '水电',
  2: '门窗',
  3: '电梯',
  4: '公共设施',
  5: '其他',
}

export const URGENCY_MAP: Record<number, string> = {
  1: '普通',
  2: '紧急',
  3: '特急',
}

// ==================== 类型 ====================

export interface RepairOrderVO {
  id: string
  orderNo: string
  ownerId: string
  roomId: string
  category: number
  categoryName: string
  title: string
  description: string
  images: string
  urgency: number
  urgencyName: string
  handlerId: string
  handleNote: string
  rejectReason: string
  rating: number
  ratingComment: string
  status: number
  statusName: string
  timeoutFlag: number
  createTime: string
  updateTime: string
}

export interface RepairOrderCreateRequest {
  title: string
  description: string
  images?: string
  category: number
  urgency: number
  roomId: number
}

export interface RepairRateRequest {
  rating: number
  ratingComment?: string
}

// ==================== 接口 ====================

export function createRepair(data: RepairOrderCreateRequest) {
  return request.post('/api/owner/service/repair', data)
}

export function myRepairs(params: { current?: number; size?: number; status?: number }) {
  return request.get('/api/owner/service/repair/mine', { params })
}

export function getRepair(id: string) {
  return request.get(`/api/owner/service/repair/${id}`)
}

export function cancelRepair(id: string) {
  return request.post(`/api/owner/service/repair/${id}/cancel`)
}

export function rateRepair(id: string, data: RepairRateRequest) {
  return request.post(`/api/owner/service/repair/${id}/rate`, data)
}

// ==================== 场地/访客枚举映射 ====================

export const VENUE_TYPE_MAP: Record<number, string> = {
  1: '健身房',
  2: '棋牌室',
  3: '会议室',
  4: '游泳池',
  5: '其他',
}

export const BOOKING_STATUS_MAP: Record<number, string> = {
  0: '已预约',
  1: '已使用',
  2: '已取消',
  3: '已违约',
}

export const VISITOR_PASS_STATUS_MAP: Record<number, string> = {
  0: '有效',
  1: '已用尽',
  2: '已过期',
  3: '已撤销',
}

// ==================== 场地类型 ====================

export interface VenueVO {
  id: string
  name: string
  venueType: number
  venueTypeName: string
  location: string
  capacity: number
  openTime: string
  closeTime: string
  slotMinutes: number
  monthlyLimit: number
  price: number
  status: number
  statusName: string
}

export interface VenueSlotVO {
  venueId: string
  date: string
  openTime: string
  closeTime: string
  slotMinutes: number
  occupied: Array<{ startTime: string; endTime: string }>
}

export interface VenueBookingVO {
  id: string
  venueId: string
  venueName: string
  ownerId: string
  bookingDate: string
  startTime: string
  endTime: string
  status: number
  statusName: string
  createTime: string
}

export interface VenueBookingRequest {
  bookingDate: string
  startTime: string
  endTime: string
}

// ==================== 场地接口 ====================

export function venueList(params?: { name?: string; venueType?: number }) {
  return request.get('/api/owner/service/venue/list', { params })
}

export function venueSlots(id: string, date: string) {
  return request.get(`/api/owner/service/venue/${id}/slots`, { params: { date } })
}

export function bookVenue(id: string, data: VenueBookingRequest) {
  return request.post(`/api/owner/service/venue/${id}/book`, data)
}

export function myVenueBookings(params: { current?: number; size?: number }) {
  return request.get('/api/owner/service/venue/booking/mine', { params })
}

export function cancelVenueBooking(id: string) {
  return request.delete(`/api/owner/service/venue/booking/${id}`)
}

// ==================== 访客通行码类型 ====================

export interface VisitorPassVO {
  id: string
  passCode: string
  ownerId: string
  visitorName: string
  visitorPhone: string
  plateNo: string
  validFrom: string
  validUntil: string
  maxUse: number
  usedCount: number
  status: number
  statusName: string
  createTime: string
}

export interface VisitorPassCreateRequest {
  visitorName: string
  visitorPhone?: string
  plateNo?: string
  validFrom: string
  validUntil: string
  maxUse: number
}

// ==================== 访客通行码接口 ====================

export function createVisitorPass(data: VisitorPassCreateRequest) {
  return request.post('/api/owner/service/visitor-pass', data)
}

export function myVisitorPasses(params: { current?: number; size?: number }) {
  return request.get('/api/owner/service/visitor-pass/mine', { params })
}

export function revokeVisitorPass(id: string) {
  return request.delete(`/api/owner/service/visitor-pass/${id}`)
}
