import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface AdminInfo {
  adminId: number
  username: string
  role: string
}

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('adminToken') || '')
  const adminInfo = ref<AdminInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('adminToken', newToken)
  }

  function setAdminInfo(info: AdminInfo) {
    adminInfo.value = info
  }

  function logout() {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('adminToken')
  }

  return {
    token,
    adminInfo,
    isLoggedIn,
    setToken,
    setAdminInfo,
    logout
  }
})
