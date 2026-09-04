import axios from 'axios'
import { showFailToast } from 'vant'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
  withCredentials: true,
})

// 刷新去重：并发 401 时只发起一次刷新
let refreshing = false
let waiters: Array<(ok: boolean) => void> = []

function onRefreshed(ok: boolean) {
  waiters.forEach((cb) => cb(ok))
  waiters = []
}

function doRefresh(): Promise<boolean> {
  if (refreshing) {
    return new Promise((resolve) => waiters.push(resolve))
  }
  refreshing = true
  return axios
    .post('/api/owner/auth/refresh', null, { withCredentials: true })
    .then((resp) => {
      const ok = resp.data?.code === 200
      onRefreshed(ok)
      return ok
    })
    .catch(() => {
      onRefreshed(false)
      return false
    })
    .finally(() => {
      refreshing = false
    })
}

function redirectLogin() {
  showFailToast('登录已过期，请重新登录')
  localStorage.removeItem('owner_id')
  localStorage.removeItem('owner_name')
  localStorage.removeItem('owner_phone')
  window.location.href = '/login'
}

// 后端所有异常均返回 HTTP 200，仅在 body.code 区分错误类型。
// 401=Token 失效，1006=业主已删除（会话不再有效），两者都视为会话失效。
function isSessionInvalid(code: number) {
  return code === 401 || code === 1006
}

// 响应拦截：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      // 会话失效：要在 session 校验请求上抑制跳转，交由路由守卫处理，避免闪现/重复跳转
      if (isSessionInvalid(res.code)) {
        const isSessionCheck = response.config?.url?.includes('/profile/session') === true
        if (!isSessionCheck) {
          redirectLogin()
          return Promise.reject(new Error(res.msg || '登录已过期'))
        }
        // 会话校验请求自身失败，仅 reject，不做 toast / 跳转
        return Promise.reject(new Error(res.msg || '会话失效'))
      }
      showFailToast(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  async (error) => {
    const { response, config } = error
    if (response && response.status === 401) {
      // 刷新接口本身返回 401 → 直接跳登录
      if (config.url?.includes('/auth/refresh')) {
        redirectLogin()
        return Promise.reject(error)
      }
      // 已重试过一次仍 401 → 跳登录
      if (config._retry) {
        redirectLogin()
        return Promise.reject(error)
      }
      config._retry = true
      const ok = await doRefresh()
      if (ok) {
        return request(config) // 用新 Access Cookie 重试原请求
      }
      redirectLogin()
      return Promise.reject(error)
    }

    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 403:
          showFailToast('无权限访问')
          break
        case 404:
          showFailToast('请求的资源不存在')
          break
        case 500:
          showFailToast('服务器异常')
          break
        default:
          showFailToast(`请求失败(${status})`)
      }
    } else {
      showFailToast('网络异常，请检查连接')
    }
    return Promise.reject(error)
  },
)

export default request