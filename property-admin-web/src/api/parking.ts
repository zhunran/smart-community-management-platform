import request from '@/utils/request'

export interface ParkingSpaceVO {
  id: number
  spaceCode: string
  spaceName: string
  spaceType: number
  area: number
  floor: string
  zone: string
  ownerId: number
  ownerName: string
  roomId: number
  roomCode: string
  rentalType: number
  monthlyFee: number
  status: number
  remark: string
  createTime: string
}

export interface ParkingBindRequest {
  spaceId: number
  ownerId: number
  roomId?: number
  rentalType: number
  remark?: string
}

export interface ParkingChangeRequest {
  spaceId: number
  newOwnerId: number
  newRoomId?: number
  remark?: string
}

export const PARKING_STATUS_MAP: Record<number, string> = {
  0: '空闲', 1: '已售', 2: '已租', 3: '临时', 4: '维修中',
}

export const RENTAL_TYPE_MAP: Record<number, string> = {
  1: '自有', 2: '租赁', 3: '临时',
}

export const SPACE_TYPE_MAP: Record<number, string> = {
  1: '标准', 2: '子母', 3: '机械', 4: '充电桩',
}

// ---- 车位管理 ----
export function listParking() {
  return request.get('/api/admin/parking/list')
}

export function getParkingDetail(id: number) {
  return request.get(`/api/admin/parking/${id}`)
}

export function bindParking(data: ParkingBindRequest) {
  return request.post('/api/admin/parking/bind', data)
}

export function changeParking(data: ParkingChangeRequest) {
  return request.put('/api/admin/parking/change', data)
}

export function unbindParking(id: number, remark?: string) {
  return request.post(`/api/admin/parking/unbind/${id}`, null, { params: { remark } })
}

// ---- 车位预警 ----
export interface ParkingWarningVO {
  id: number
  spaceId: number
  spaceCode: string
  spaceName: string
  warningType: string
  warningTypeName: string
  warningLevel: string
  description: string
  status: number
  statusName: string
  handler: string
  handleRemark: string
  handleTime: string
  batchNo: string
  createTime: string
}

export interface WarningHandleRequest {
  handleRemark: string
}

export function pageWarning(params: { current?: number; size?: number; warningType?: string; status?: number }) {
  return request.get('/api/admin/parking/reconciliation/page', { params })
}

export function runReconciliation() {
  return request.post('/api/admin/parking/reconciliation/run')
}

export function handleWarning(id: number, data: WarningHandleRequest) {
  return request.post(`/api/admin/parking/reconciliation/${id}/handle`, data)
}

export function closeWarning(id: number, remark?: string) {
  return request.post(`/api/admin/parking/reconciliation/${id}/close`, null, { params: { remark } })
}
