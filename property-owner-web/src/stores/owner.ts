import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { OwnerLoginRequest } from '@/api/auth'
import { loginApi, logoutApi } from '@/api/auth'
import { updateProfile as updateProfileApi } from '@/api/profile'
import type { OwnerProfileUpdateRequest } from '@/api/profile'
import { showSuccessToast } from 'vant'
import { resetSessionValidation } from '@/utils/session'

export const useOwnerStore = defineStore('owner', () => {
  // T43 迁移：旧版将 Token 存于 localStorage，现改用 httpOnly Cookie。
  // 检测到旧 token 时清除全部旧登录态，强制重新登录以写入 Cookie。
  if (localStorage.getItem('owner_token')) {
    localStorage.removeItem('owner_token')
    localStorage.removeItem('owner_id')
    localStorage.removeItem('owner_name')
    localStorage.removeItem('owner_phone')
  }

  const ownerId = ref(Number(localStorage.getItem('owner_id') || '0'))
  const ownerName = ref(localStorage.getItem('owner_name') || '')
  const phone = ref(localStorage.getItem('owner_phone') || '')

  async function login(loginData: OwnerLoginRequest) {
    const res = await loginApi(loginData)
    const data = res.data as any

    ownerId.value = data.ownerId
    ownerName.value = data.ownerName
    phone.value = data.phone

    // Token 由后端写入 httpOnly Cookie，前端不存储
    localStorage.setItem('owner_id', String(data.ownerId))
    localStorage.setItem('owner_name', data.ownerName)
    localStorage.setItem('owner_phone', data.phone)

    // 登录成功：重置会话校验，使路由守卫重新校验新的登录态
    resetSessionValidation()

    showSuccessToast('登录成功')
    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 即使后端登出失败也清除前端状态
    }
    ownerId.value = 0
    ownerName.value = ''
    phone.value = ''
    localStorage.removeItem('owner_id')
    localStorage.removeItem('owner_name')
    localStorage.removeItem('owner_phone')
    // 登出：重置会话校验，避免残留的“已校验有效”状态
    resetSessionValidation()
  }

  /**
   * 是否已登录
   * Token 在 httpOnly Cookie 中，前端无法读取，
   * 以本地是否有业主信息作为登录态标识（后端会校验 Token 有效性）
   */
  function isLoggedIn(): boolean {
    return !!ownerName.value
  }

  async function updateProfile(data: OwnerProfileUpdateRequest) {
    await updateProfileApi(data)
    ownerName.value = data.ownerName
    phone.value = data.phone
    localStorage.setItem('owner_name', data.ownerName)
    localStorage.setItem('owner_phone', data.phone)
    showSuccessToast('个人信息已更新')
  }

  return { ownerId, ownerName, phone, login, logout, isLoggedIn, updateProfile }
})
