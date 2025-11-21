<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import axios from 'axios'

/**
 * 应用启动时获取CSRF Token
 * 用于防止跨站请求伪造(CSRF)攻击
 */
onMounted(async () => {
  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    const res = await axios.get(`${baseURL}/api/auth/csrf-token`, {
      withCredentials: true
    })

    if (res.data && res.data.data && res.data.data.csrfToken) {
      // 将CSRF Token存储到meta标签中,供request拦截器使用
      let meta = document.querySelector('meta[name="csrf-token"]') as HTMLMetaElement

      if (!meta) {
        meta = document.createElement('meta')
        meta.setAttribute('name', 'csrf-token')
        document.head.appendChild(meta)
      }

      meta.setAttribute('content', res.data.data.csrfToken)
      console.log('CSRF Token loaded successfully')
    }
  } catch (error) {
    console.error('Failed to fetch CSRF token:', error)
    // CSRF Token获取失败不应阻止应用启动
  }
})
</script>

<style scoped>
</style>
