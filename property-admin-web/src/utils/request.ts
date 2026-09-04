import axios from 'axios'
import { ElMessage } from 'element-plus'

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
    .post('/api/admin/auth/refresh', null, { withCredentials: true })
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
  ElMessage.error('登录已过期，请重新登录')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  sessionStorage.removeItem('_auth_verified')
  window.location.href = '/login'
}

// 响应拦截：统一错误处理
request.interceptors.response.use(
  (response) => {
    // 对 blob / arraybuffer 等非 JSON 响应跳过 code 检查
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }

    const res = response.data
    // 后端返回 code=200 视为成功
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
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
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器异常')
          break
        default:
          ElMessage.error(`请求失败(${status})`)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  },
)

export default request