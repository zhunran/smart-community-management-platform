import request from '@/utils/request'

export interface PaymentOrderVO {
  id: number
  paymentNo: string
  billId: number
  roomId: number
  ownerId: number
  paymentMethod: number
  paymentMethodName: string
  paymentAmount: number
  paymentTime: string
  transactionId: string
  paymentStatus: number
  paymentStatusName: string
  refundAmount: number
  payerName: string
  payerPhone: string
  remark: string
  createTime: string
  buildingName: string
  roomCode: string
  roomName: string
  ownerName: string
  ownerPhone: string
  billNo: string
  billPeriod: string
}

export interface PaymentOrderPageQuery {
  current?: number
  size?: number
  billId?: number
  roomId?: number
  ownerId?: number
  paymentMethod?: number
  paymentStatus?: number
  paymentNo?: string
  payerName?: string
  paymentTimeStart?: string
  paymentTimeEnd?: string
}

export function pagePayment(params: PaymentOrderPageQuery) {
  return request.get('/api/admin/payments/page', { params })
}

export function getPaymentDetail(id: number) {
  return request.get(`/api/admin/payments/${id}`)
}

export function syncPayment(paymentNo: string) {
  return request.post(`/api/admin/payments/${paymentNo}/sync`)
}

export function exportPaymentsExcel(params: PaymentOrderPageQuery) {
  return request.get('/api/admin/payments/export', { params, responseType: 'blob' })
}

export function importPaymentsExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/payments/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}
