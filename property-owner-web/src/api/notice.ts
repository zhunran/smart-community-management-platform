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

export const NOTICE_TYPE_MAP: Record<string, string> = {
  NOTICE: '社区公告',
  WATER_ELECTRIC: '水电通知',
  ACTIVITY: '社区活动',
  EMERGENCY: '紧急通知',
}

export function getNotices() {
  return request.get<NoticeVO[]>('/api/owner/notices')
}