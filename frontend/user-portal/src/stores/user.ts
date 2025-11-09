import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '../api'
 
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
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(loadUserInfo())
  // 兼容部分组件使用的名称：user
  const user = computed(() => userInfo.value)
 
  const isLoggedIn = computed(() => !!token.value)
 
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }
 
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    try {
      localStorage.setItem('userInfo', JSON.stringify(info))
    } catch {
      // ignore
    }
  }
 
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }
 
  return {
    token,
    userInfo,
    user,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout
  }
})
