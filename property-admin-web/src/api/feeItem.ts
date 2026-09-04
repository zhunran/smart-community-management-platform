import request from '@/utils/request'

export interface FeeItemVO {
  id: number
  itemCode: string
  itemName: string
  billingCycle: number
  calcType: number
  unitPrice: number
  sortOrder: number
  status: number
  remark: string
  createTime: string
  updateTime: string
}

export interface FeeItemPageQuery {
  current?: number
  size?: number
  itemCode?: string
  itemName?: string
  billingCycle?: number
  calcType?: number
  status?: number
}

export interface FeeItemCreateRequest {
  itemCode: string
  itemName: string
  billingCycle?: number
  calcType?: number
  unitPrice?: number
  sortOrder?: number
  status: number
  remark?: string
}

export interface FeeItemUpdateRequest {
  id: number
  itemCode: string
  itemName: string
  billingCycle?: number
  calcType?: number
  unitPrice?: number
  sortOrder?: number
  status: number
  remark?: string
}

export function pageFeeItem(params: FeeItemPageQuery) {
  return request.get('/api/admin/fee-item/page', { params })
}

export function getFeeItemDetail(id: number) {
  return request.get(`/api/admin/fee-item/${id}`)
}

export function listAllFeeItems() {
  return request.get('/api/admin/fee-item/list')
}

export function createFeeItem(data: FeeItemCreateRequest) {
  return request.post('/api/admin/fee-item', data)
}

export function updateFeeItem(data: FeeItemUpdateRequest) {
  return request.put('/api/admin/fee-item', data)
}

export function deleteFeeItem(id: number) {
  return request.delete(`/api/admin/fee-item/${id}`)
}
