import request from '@/utils/request'

export interface NoticeVO {
  id: number
  title: string
  content: string
  type: string
  status: number
  publishTime: string
  createTime: string
}

export interface NoticePageQuery {
  current?: number
  size?: number
  type?: string
  status?: number
}

export interface NoticeCreateRequest {
  title: string
  content: string
  type?: string
}

export const NOTICE_TYPE_MAP: Record<string, string> = {
  NOTICE: '社区公告',
  WATER_ELECTRIC: '水电通知',
  ACTIVITY: '社区活动',
  EMERGENCY: '紧急通知',
}

export const NOTICE_STATUS_MAP: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已下线',
}

export function pageNotice(params: NoticePageQuery) {
  return request.get('/api/admin/notice/page', { params })
}

export function createNotice(data: NoticeCreateRequest) {
  return request.post('/api/admin/notice', data)
}

export function publishNotice(id: number) {
  return request.put(`/api/admin/notice/${id}/publish`)
}

export function offlineNotice(id: number) {
  return request.put(`/api/admin/notice/${id}/offline`)
}