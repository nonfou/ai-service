import axios, { type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 定义后端响应结构
interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 返回 ApiResponse 类型
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 如果返回的状态码不是200,则认为是错误
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 直接返回 ApiResponse
    return res as any
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        ElMessage.error('登录已过期,请重新登录')
        localStorage.removeItem('adminToken')
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status === 500) {
        ElMessage.error(data?.message || '服务器错误')
      } else {
        ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误,请检查您的网络连接')
    }

    return Promise.reject(error)
  }
)

export default request
