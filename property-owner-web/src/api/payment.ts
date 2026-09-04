import request from '@/utils/request'

export interface PayOrderRequest {
  billId: number | string
  paymentMethod: number
  payerName?: string
  remark?: string
}

export interface PayOrderResult {
  paymentId: number
  paymentNo: string
  paymentMethod: number
  paymentMethodName: string
  onlinePay: boolean
  payFormHtml: string
}

export interface PaymentOrderVO {
  id: number
  paymentNo: string
  billId: number
  paymentMethod: number
  paymentMethodName: string
  paymentAmount: number
  paymentTime: string
  transactionId: string
  paymentStatus: number
  paymentStatusName: string
  payerName: string
  remark: string
  createTime: string
  billPeriod: string
  billNo: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function createPayOrder(data: PayOrderRequest) {
  return request.post<PayOrderResult>('/api/owner/payment/create', data)
}

export function getPaymentPage(params: { current: number; size: number; paymentStatus?: number }) {
  return request.get<PageResult<PaymentOrderVO>>('/api/owner/payment/page', { params })
}

export function getPaymentDetail(id: number) {
  return request.get<PaymentOrderVO>(`/api/owner/payment/${id}`)
}
