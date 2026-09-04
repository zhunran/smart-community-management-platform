import request from '@/utils/request'

export interface BillVO {
  id: string
  billNo: string
  roomId: number
  ownerId: number
  billPeriod: string
  billType: number
  billDate: string
  dueDate: string
  totalAmount: number
  paidAmount: number
  discountAmount: number
  lateFee: number
  status: number
  createTime: string
  roomCode: string
  buildingName: string
  ownerName: string
  statusName?: string
}

export interface BillItemVO {
  id: string
  billId: string
  feeItemId: number
  feeItemName: string
  calcBase: number
  unitPrice: number
  quantity: number
  amount: number
  paidAmount: number
}

export interface BillDetailVO {
  id: string
  billNo: string
  roomId: number
  ownerId: number
  billPeriod: string
  billDate: string
  dueDate: string
  totalAmount: number
  paidAmount: number
  discountAmount: number
  lateFee: number
  status: number
  remark: string
  createTime: string
  roomCode: string
  buildingName: string
  ownerName: string
  items: BillItemVO[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function getBillPage(params: { current: number; size: number; status?: number; billPeriod?: string }) {
  return request.get<PageResult<BillVO>>('/api/owner/bills/page', { params })
}

export function getBillDetail(id: string) {
  return request.get<BillDetailVO>(`/api/owner/bills/${id}`)
}
