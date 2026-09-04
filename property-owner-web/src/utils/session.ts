import { getSessionApi } from '@/api/auth'

/**
 * 会话校验：记忆化单次校验结果，应用生命周期内只会真正请求一次后端。
 * 登录成功后重置，避免每次路由跳转都发起校验请求。
 */
let sessionChecked = false
let sessionValid = false
let checking: Promise<boolean> | null = null

/** 重置会话校验状态（登录成功 / 登出 / 会话失效时调用） */
export function resetSessionValidation() {
  sessionChecked = false
  sessionValid = false
  checking = null
}

/** 校验当前会话是否有效（Token 有效且业主存在）。无效返回 false */
export async function validateSession(): Promise<boolean> {
  if (sessionChecked) return sessionValid
  if (checking) return checking
  checking = (async () => {
    try {
      await getSessionApi()
      sessionValid = true
    } catch {
      sessionValid = false
    }
    sessionChecked = true
    checking = null
    return sessionValid
  })()
  return checking
}