import request from '@/utils/request'

// ===== 缴费趋势 =====
export interface FeeTrendPoint {
  date: string // yyyy-MM-dd
  payerCount: number
  amount: number
}

// ===== 操作审计 =====
export interface AuditLogItem {
  id: string
  traceId: string
  userName: string
  realName: string
  module: string
  action: string
  requestMethod: string
  requestUrl: string
  ipAddress: string
  status: 0 | 1 // 1成功 0失败
  resultCode: number
  resultMsg: string
  costTime: number
  createTime: string
}

export interface AuditLogQuery {
  pageNum?: number
  pageSize?: number
  start?: string // yyyy-MM-ddTHH:mm:ss
  end?: string
  status?: 0 | 1
  module?: string
  userName?: string
  keyword?: string
}

export interface AuditModuleStat {
  module: string
  count: number
}

export interface AuditUserStat {
  userName: string
  realName: string
  count: number
}

export interface AuditSummary {
  totalCount: number
  failCount: number
  failRate: number // 百分比
  moduleTop: AuditModuleStat[]
  userTop: AuditUserStat[]
  riskActionCount: Record<string, number>
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ===== 接口 =====
export function getFeeTrend(start?: string, end?: string) {
  return request.get('/api/admin/statistic/fee/trend', { params: { start, end } })
}

export function getAuditLogs(params: AuditLogQuery) {
  return request.get('/api/admin/statistic/audit/log', { params })
}

export function getAuditSummary(params: AuditLogQuery) {
  return request.get('/api/admin/statistic/audit/summary', { params })
}

export function exportAuditLogs(params: AuditLogQuery) {
  return request.get('/api/admin/statistic/audit/export', { params, responseType: 'blob' })
}