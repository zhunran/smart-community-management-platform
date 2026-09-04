import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginRequest, LoginResponse } from '@/api/auth'
import { loginApi, logoutApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // T43 迁移：旧版将 Token 存于 localStorage，现改用 httpOnly Cookie。
  // 检测到旧 token 时清除全部旧登录态，强制重新登录以写入 Cookie。
  if (localStorage.getItem('token')) {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
  }

  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('realName') || '')

  /** 登录 */
  async function login(loginData: LoginRequest) {
    const res = await loginApi(loginData)
    const data = res.data as LoginResponse

    username.value = data.username
    realName.value = data.realName

    // Token 由后端写入 httpOnly Cookie，前端不存储
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName)

    ElMessage.success('登录成功')
    return data
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 即使后端登出失败也清除前端状态
    }
    clearUser()
  }

  /** 清除用户状态（登出或 token 过期时调用） */
  function clearUser() {
    username.value = ''
    realName.value = ''
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
    sessionStorage.removeItem('_auth_verified')
  }

  /**
   * 是否已登录
   * Token 在 httpOnly Cookie 中，前端无法读取，
   * 以本地是否有用户信息作为登录态标识（后端会校验 Token 有效性）
   */
  function isLoggedIn(): boolean {
    return !!username.value
  }

  return { username, realName, login, logout, clearUser, isLoggedIn }
})
