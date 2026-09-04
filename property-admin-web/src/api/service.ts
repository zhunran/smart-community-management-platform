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

export interface RepairOrderQuery {
  current?: number
  size?: number
  category?: number
  status?: number
  timeoutFlag?: number
  keyword?: string
}

export interface RepairStatisticsVO {
  total: number
  statusCounts: Record<number, number>
  avgHandleHours: number
}

// ==================== 接口 ====================

export function pageRepair(params: RepairOrderQuery) {
  return request.get('/api/admin/service/repair/page', { params })
}

export function getRepair(id: string) {
  return request.get(`/api/admin/service/repair/${id}`)
}

export function auditRepair(id: string, approved: boolean, reason?: string) {
  return request.post(`/api/admin/service/repair/${id}/audit`, { approved, reason })
}

export function assignRepair(id: string, handlerId: string) {
  return request.post(`/api/admin/service/repair/${id}/assign`, { handlerId: Number(handlerId) })
}

export function acceptRepair(id: string) {
  return request.post(`/api/admin/service/repair/${id}/accept`)
}

export function completeRepair(id: string, handleNote: string) {
  return request.post(`/api/admin/service/repair/${id}/complete`, { handleNote })
}

export function repairStatistics() {
  return request.get('/api/admin/service/repair/statistics')
}

// ==================== 场地/访客枚举映射 ====================

export const VENUE_TYPE_MAP: Record<number, string> = {
  1: '健身房',
  2: '棋牌室',
  3: '会议室',
  4: '游泳池',
  5: '其他',
}

export const VENUE_STATUS_MAP: Record<number, string> = {
  0: '停用',
  1: '启用',
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

export interface VenueQuery {
  current?: number
  size?: number
  name?: string
  venueType?: number
  status?: number
}

export interface VenueCreateRequest {
  name: string
  venueType: number
  location?: string
  capacity: number
  openTime: string
  closeTime: string
  slotMinutes: number
  monthlyLimit?: number
  price?: number
  status?: number
}

export interface VenueUpdateRequest extends VenueCreateRequest {
  id: string
}

// ==================== 场地接口 ====================

export function pageVenue(params: VenueQuery) {
  return request.get('/api/admin/service/venue/page', { params })
}

export function getVenue(id: string) {
  return request.get(`/api/admin/service/venue/${id}`)
}

export function createVenue(data: VenueCreateRequest) {
  return request.post('/api/admin/service/venue', data)
}

export function updateVenue(data: VenueUpdateRequest) {
  return request.put('/api/admin/service/venue', data)
}

export function deleteVenue(id: string) {
  return request.delete(`/api/admin/service/venue/${id}`)
}

export function pageVenueBooking(params: { current?: number; size?: number; venueId?: string }) {
  return request.get('/api/admin/service/venue/booking/page', { params })
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

export interface VisitorPassQuery {
  current?: number
  size?: number
  status?: number
  keyword?: string
}

export interface VisitorPassVerifyVO {
  valid: boolean
  message: string
  passCode: string
  visitorName: string
  plateNo: string
  usedCount: number
  maxUse: number
}

// ==================== 访客通行码接口 ====================

export function pageVisitorPass(params: VisitorPassQuery) {
  return request.get('/api/admin/service/visitor-pass/page', { params })
}

export function verifyVisitorPass(passCode: string) {
  return request.post('/api/admin/service/visitor-pass/verify', { passCode })
}
