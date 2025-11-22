<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import axios from 'axios'

onMounted(async () => {
  try {
    const baseURL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
    const res = await axios.get(`${baseURL}/api/auth/csrf-token`, {
      withCredentials: true
    })

    const token = res.data?.data?.csrfToken
    if (!token) {
      return
    }

    let meta = document.querySelector('meta[name="csrf-token"]') as HTMLMetaElement | null
    if (!meta) {
      meta = document.createElement('meta')
      meta.setAttribute('name', 'csrf-token')
      document.head.appendChild(meta)
    }
    meta.setAttribute('content', token)
  } catch (error) {
    console.error('获取 CSRF Token 失败', error)
  }
})
</script>

<style>
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
