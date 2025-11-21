import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  withCredentials: true  // ✅ 允许携带Cookie (HttpOnly)
})

/**
 * 从 Cookie 中获取指定名称的值
 * @param name Cookie名称
 * @returns Cookie值,如果不存在返回null
 */
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null
  }
  return null
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // ✅ Token现在通过HttpOnly Cookie自动携带,无需手动添加
    // ❌ 删除: const token = localStorage.getItem('token')
    // ❌ 删除: config.headers.Authorization = `Bearer ${token}`

    // ✅ 添加CSRF Token保护
    const csrfToken =
      document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') ||
      getCookie('XSRF-TOKEN')

    if (csrfToken) {
      config.headers['X-CSRF-Token'] = csrfToken
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      // 对于业务错误(200状态但code不是200),不在拦截器显示错误
      // 让具体页面自己处理
      switch (status) {
        case 401:
          ElMessage.error('登录已过期,请重新登录')
          // ❌ 删除: localStorage.removeItem('token')
          // ✅ Cookie会被后端自动清除
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          // 不在拦截器显示错误,让页面自己处理
          break
        default:
          // 不在拦截器显示错误,让页面自己处理
          break
      }
    } else if (error.request) {
      ElMessage.error('网络错误,请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default request
