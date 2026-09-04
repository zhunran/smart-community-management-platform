import request from '@/utils/request'

export interface DashboardVO {
  currentMonthReceivable: number
  currentMonthReceived: number
  totalArrears: number
  collectionRate: number
  statisticsMonth: string
}

export interface FeeItemStatVO {
  feeItemId: number
  feeItemName: string
  receivable: number
  received: number
  billCount: number
  paidCount: number
  collectionRate: number
}

export function getDashboardOverview(period?: string) {
  return request.get('/api/admin/dashboard/overview', { params: { period } })
}

export function getFeeItemStats(period?: string) {
  return request.get('/api/admin/dashboard/fee-item-stats', { params: { period } })
}

export function exportMonthlyReport(period: string) {
  return request.get('/api/admin/report/export/monthly', { params: { period }, responseType: 'blob' })
}
