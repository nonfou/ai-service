<template>
  <router-view />
</template>

<script setup lang="ts">
// 系统使用 STATELESS JWT + SameSite Cookie，无需 CSRF Token
</script>

<style>
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
