import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  userType: number
}

export function loginApi(data: LoginRequest) {
  return request.post<LoginResponse>('/api/admin/auth/login', data)
}

export function logoutApi() {
  return request.post('/api/admin/auth/logout')
}
