import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '../router'

// 获取 API 基础地址
// 空字符串表示使用相对路径（同源代理），避免 CORS 问题
const getBaseURL = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL
  // 明确设置了值（包括空字符串）则使用该值，否则使用开发环境默认值
  if (envUrl !== undefined) {
    return envUrl
  }
  return '/api'
}

const request = axios.create({
  baseURL: getBaseURL(),
  timeout: 15000,
  withCredentials: true
})

const redirectToLogin = async () => {
  try {
    const { useUserStore } = await import('../stores/user')
    const userStore = useUserStore()
    userStore.clearLoginState()
  } catch (error) {
    console.error('清理用户登录状态失败:', error)
  }

  ElMessage.closeAll()

  const currentRoute = router.currentRoute.value
  const redirectParam =
    currentRoute?.path && currentRoute.path !== '/login'
      ? { redirect: currentRoute.fullPath }
      : undefined

  try {
    await router.replace({ path: '/login', query: redirectParam })
  } catch (err) {
    console.error('路由跳转失败,尝试直接跳转:', err)
    const resolved = router.resolve({ path: '/login', query: redirectParam })
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
      cancelButtonText: '暂不',
      type: 'warning',
      closeOnClickModal: false,
      closeOnPressEscape: false
    }
  )
    .then(() => redirectToLogin())
    .catch(() => {
      // 用户取消,保留当前页面
    })
    .finally(() => {
      authPromptPromise = null
    })

  return authPromptPromise
}

/**
 * 从 Cookie 中获取指定名称的值
 */
const getCookie = (name: string): string | null => {
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
  async (response) => {
    const res = response.data

    if (res && typeof res === 'object' && 'code' in res && res.code === 401) {
      const prompt = promptReLogin()
      if (prompt) {
        await prompt.catch(() => {})
      }
      return Promise.reject(res)
    }

    return res
  },
  async (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
        case 403: {
          const prompt = promptReLogin()
          if (prompt) {
            await prompt.catch(() => {})
          }
          break
        }
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          // 交由业务页面自行处理
          break
        default:
          // 交由业务页面自行处理
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
