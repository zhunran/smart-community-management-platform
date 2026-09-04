import request from '@/utils/request'

export interface BillVO {
  id: number
  billNo: string
  roomId: number
  ownerId: number
  billPeriod: string
  billType: number
  billDate: string
  dueDate: string
  totalAmount: number
  paidAmount: number
  status: number
  createTime: string
  roomCode: string
  roomName: string
  ownerName: string
  ownerPhone: string
  buildingId: number
  buildingName: string
}

export interface BillItemVO {
  id: number
  feeItemId: number
  feeItemName: string
  calcBase: number
  unitPrice: number
  quantity: number
  amount: number
  discountAmount: number
  paidAmount: number
  remark: string
}

export interface BillDetailVO {
  id: number
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
  remark: string
  createTime: string
  roomCode: string
  roomName: string
  ownerName: string
  ownerPhone: string
  items: BillItemVO[]
}

export interface BillPageQuery {
  current?: number
  size?: number
  roomId?: number
  ownerId?: number
  buildingId?: number
  billPeriod?: string
  billPeriodStart?: string
  billPeriodEnd?: string
  billType?: number
  status?: number
  hasParkingFee?: boolean
  roomCode?: string
}

export interface BillGenerateRequest {
  billPeriod: string
  ownerId?: number
  roomIds?: number[]
  dueDate?: string
}

export interface ManualPaymentRequest {
  billId: number
  paymentMethod: number
  payerName?: string
  remark?: string
}

export interface ItemPayment {
  billItemId: number
  amount: number
}

export interface ItemizedPaymentRequest {
  billId: number
  items: ItemPayment[]
  paymentMethod: number
  payerName?: string
  remark?: string
}

export const BILL_STATUS_MAP: Record<number, string> = {
  0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废', 4: '已减免',
}

export function pageBill(params: BillPageQuery) {
  return request.get('/api/admin/bills/page', { params })
}

export function getBillDetail(id: number) {
  return request.get(`/api/admin/bills/${id}`)
}

export function generateBill(data: BillGenerateRequest) {
  return request.post('/api/admin/bills/generate', data)
}

export function manualPayment(data: ManualPaymentRequest) {
  return request.post('/api/admin/bills/manual-payment', data)
}

export function itemizedPayment(data: ItemizedPaymentRequest) {
  return request.post('/api/admin/bills/itemized-payment', data)
}
