import request from '@/utils/request'

/**
 * 刷新 Access Token（供前端显式调用，如登录状态检查）
 */
export function refreshApi() {
  return request.post('/api/admin/auth/refresh')
}