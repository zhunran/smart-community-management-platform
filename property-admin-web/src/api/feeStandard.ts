import request from '@/utils/request'

export interface FeeStandardVO {
  id: number
  roomId: number
  roomCode: string
  feeItemId: number
  feeItemName: string
  unitPrice: number
  startDate: string
  endDate: string
  status: number
  remark: string
  createTime: string
}

export interface FeeStandardCreateRequest {
  roomId?: number
  feeItemId: number
  unitPrice: number
  startDate?: string
  endDate?: string
  remark?: string
}

export interface FeeStandardUpdateRequest {
  id: number
  unitPrice?: number
  startDate?: string
  endDate?: string
  status?: number
  remark?: string
}

export function pageFeeStandard(params: { current?: number; size?: number; feeItemId?: number; roomId?: number; status?: number }) {
  return request.get('/api/admin/fee-standard/page', { params })
}

export function listFeeStandardsByFeeItem(feeItemId: number) {
  return request.get('/api/admin/fee-standard/list-by-fee-item', { params: { feeItemId } })
}

export function createFeeStandard(data: FeeStandardCreateRequest) {
  return request.post('/api/admin/fee-standard', data)
}

export function updateFeeStandard(data: FeeStandardUpdateRequest) {
  return request.put('/api/admin/fee-standard', data)
}

export function deleteFeeStandard(id: number) {
  return request.delete(`/api/admin/fee-standard/${id}`)
}
