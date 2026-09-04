import request from '@/utils/request'

export interface OwnerLoginRequest {
  phone: string
  password: string
  captcha: string
}

export interface OwnerLoginResponse {
  token: string
  ownerId: number
  ownerName: string
  phone: string
  role: string
}

export function loginApi(data: OwnerLoginRequest) {
  return request.post<OwnerLoginResponse>('/api/owner/auth/login', data)
}

export function logoutApi() {
  return request.post('/api/owner/auth/logout')
}

/** 会话校验：Token 有效且业主存在时返回成功 */
export function getSessionApi() {
  return request.get('/api/owner/profile/session')
}
