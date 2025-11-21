import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '../api'
import request from '../utils/request'

// 从 localStorage 读取 userInfo 的辅助函数
function loadUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  // ❌ 删除: const token = ref<string>(localStorage.getItem('token') || '')
  // ✅ Token现在存储在HttpOnly Cookie中,前端无法访问

  // ✅ 改为直接使用 ref,初始值为 false
  const isLoggedIn = ref<boolean>(false)
  const userInfo = ref<UserInfo | null>(loadUserInfo())

  // 兼容部分组件使用的名称：user
  const user = computed(() => userInfo.value)

  // ❌ 删除 setToken 函数,不再需要
  // function setToken(newToken: string) { ... }

  /**
   * 检查当前登录状态
   * 通过调用后端API,验证HttpOnly Cookie中的token是否有效
   */
  async function checkLoginStatus() {
    try {
      const res = await request.get<any, { data: { isLoggedIn: boolean, userInfo?: UserInfo } }>('/api/auth/status')
      isLoggedIn.value = res.data.isLoggedIn || false

      if (res.data.userInfo) {
        setUserInfo(res.data.userInfo)
      } else if (!res.data.isLoggedIn) {
        userInfo.value = null
        localStorage.removeItem('userInfo')
      }
    } catch (error) {
      console.error('Failed to check login status:', error)
      isLoggedIn.value = false
      userInfo.value = null
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    try {
      localStorage.setItem('userInfo', JSON.stringify(info))
    } catch {
      // ignore
    }
  }

  async function logout() {
    // 先清理前端状态
    isLoggedIn.value = false
    userInfo.value = null
    localStorage.removeItem('userInfo')
    // ❌ 删除: localStorage.removeItem('token')

    // ✅ 调用后端API清除HttpOnly Cookie
    try {
      await request.post('/api/auth/logout')
      console.log('Logout successful, Cookie cleared')
    } catch (error: any) {
      // 即使API调用失败,前端状态已清理,认为登出成功
      console.warn('Logout API failed, but local state cleared:', error)

      // 如果是403错误,可能是CSRF问题(虽然现在已修复)
      if (error.response?.status === 403) {
        console.error('CSRF validation failed for logout - this should not happen after fix')
      }
    }
  }

  return {
    // ❌ 删除: token
    isLoggedIn,
    userInfo,
    user,
    // ❌ 删除: setToken
    checkLoginStatus,
    setUserInfo,
    logout
  }
})
