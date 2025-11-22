import axios, { type AxiosResponse } from 'axios'
import { ElMessageBox } from 'element-plus'
import router from '../router'
import message from './message'

// 定义后端响应结构
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000,
  withCredentials: true
})

const redirectToLogin = async () => {
  localStorage.removeItem('adminToken')
  message.reset()

  const currentRoute = router.currentRoute.value
  const query =
    currentRoute?.path && currentRoute.path !== '/login'
      ? { redirect: currentRoute.fullPath }
      : {}

  try {
    await router.replace({ path: '/login', query })
  } catch (err) {
    console.error('路由跳转失败:', err)
    const resolved = router.resolve({ path: '/login', query })
    window.location.assign(resolved.href)
  }
}

let authPromptPromise: Promise<void> | null = null

const promptReLogin = () => {
  if (authPromptPromise) {
    return authPromptPromise
  }

  authPromptPromise = ElMessageBox.confirm(
    '登录信息已失效，是否立即前往登录页？',
    '登录已过期',
    {
      confirmButtonText: '重新登录',
      cancelButtonText: '稍后再说',
      type: 'warning',
      closeOnClickModal: false,
      closeOnPressEscape: false
    }
  )
    .then(() => redirectToLogin())
    .catch(() => {
      // 用户取消, 不做跳转
    })
    .finally(() => {
      authPromptPromise = null
    })

  return authPromptPromise
}

const getCookie = (name: string): string | null => {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null
  }
  return null
}

const buildBusinessError = (response: AxiosResponse<ApiResponse>) => {
  const error = new Error(response.data?.message || '请求失败')
  Object.assign(error, {
    response,
    isBusinessError: true
  })
  return error
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    const csrfToken =
      typeof document !== 'undefined'
        ? document.querySelector('meta[name=\"csrf-token\"]')?.getAttribute('content') ||
          getCookie('XSRF-TOKEN')
        : null

    if (csrfToken) {
      config.headers['X-CSRF-Token'] = csrfToken
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 返回 ApiResponse 类型
request.interceptors.response.use(
  async (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 如果返回的状态码不是200,则认为是错误
    if (res.code !== undefined && res.code !== 200) {
      if (res.code === 401) {
        const confirmPromise = promptReLogin()
        // 等待跳转完成
        if (confirmPromise) {
          await confirmPromise.catch(() => {})
        }
      } else {
        message.error(res.message || '请求失败')
      }
      return Promise.reject(buildBusinessError(response))
    }

    // 直接返回 ApiResponse
    return res as any
  },
  async (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401 || status === 403) {
        const confirmPromise = promptReLogin()
        if (confirmPromise) {
          await confirmPromise.catch(() => {})
        }
      } else if (status === 404) {
        message.error('请求的资源不存在')
      } else if (status === 500) {
        message.error(data?.message || '服务器错误')
      } else {
        message.error(data?.message || '请求失败')
      }
    } else {
      message.error('网络错误,请检查您的网络连接')
    }

    return Promise.reject(error)
  }
)

export default request
