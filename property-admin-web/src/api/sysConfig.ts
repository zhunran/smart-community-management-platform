import request from '@/utils/request'

export interface SysConfigVO {
  id: string
  configKey: string
  configValue: string
  configType: number
  groupName: string
  description: string
  status: number
  remark: string
}

export interface SysConfigUpdateRequest {
  configKey: string
  configValue: string
  status: number
}

export function pageSysConfig(params: { current: number; size: number; groupName?: string; configKey?: string }) {
  return request.get('/api/admin/sys-config/page', { params })
}

export function updateSysConfig(id: string, data: SysConfigUpdateRequest) {
  return request.put(`/api/admin/sys-config/${id}`, data)
}

export function refreshSysConfigCache(id: string, configKey: string) {
  return request.post(`/api/admin/sys-config/${id}/refresh-cache`, null, { params: { configKey } })
}