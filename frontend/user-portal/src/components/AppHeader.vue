<template>
  <header class="app-header">
    <div class="header-container">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-text">xCoder</span>
      </router-link>

      <!-- 导航链接 -->
      <nav class="nav-links">
        <!-- 未登录时显示 -->
        <template v-if="!userStore.isLoggedIn">
          <router-link to="/getting-started" class="nav-link">快速开始</router-link>
          <router-link to="/models" class="nav-link">模型</router-link>
          <router-link to="/subscriptions" class="nav-link">订阅套餐</router-link>
          <router-link to="/documentation" class="nav-link">文档</router-link>
        </template>

        <!-- 登录后显示 -->
        <template v-else>
          <router-link to="/dashboard" class="nav-link">控制台</router-link>
          <router-link to="/models" class="nav-link">模型</router-link>
          <router-link to="/subscriptions" class="nav-link">订阅套餐</router-link>
          <router-link to="/tickets" class="nav-link">工单</router-link>
          <router-link to="/documentation" class="nav-link">文档</router-link>
        </template>
      </nav>

      <!-- 右侧操作区 -->
      <div class="header-actions">
        <!-- 用户菜单 -->
        <!-- 显示条件：已登录 -->
        <el-dropdown v-if="userStore.isLoggedIn" trigger="click" class="user-dropdown">
          <button class="user-btn">
            <div class="user-avatar">{{ getUserInitial() }}</div>
            <span class="user-name">{{ userStore.userInfo?.username || userStore.userInfo?.email || '用户' }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 登录按钮 -->
        <router-link v-else to="/login" class="login-btn">
          登录
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const getUserInitial = () => {
  const username = userStore.userInfo?.username || userStore.userInfo?.email || '用户'
  return username.charAt(0).toUpperCase()
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  // 登出后跳转到登录页
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: var(--z-index-sticky);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(229, 231, 235, 0.8);
  height: var(--header-height);
}

.header-container {
  max-width: var(--container-max-width);
  margin: 0 auto;
  padding: 0 var(--spacing-6);
  height: 100%;
  display: flex;
  align-items: center;
  gap: var(--spacing-8);
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  text-decoration: none;
  flex-shrink: 0;
}

.logo-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
}

/* 导航链接 */
.nav-links {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  flex: 1;
}

.nav-link {
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.nav-link:hover {
  background: var(--color-gray-100);
  color: var(--color-text-primary);
}

.nav-link-active,
.nav-link.router-link-active {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  font-weight: var(--font-weight-semibold);
}

.nav-link-active:hover,
.nav-link.router-link-active:hover {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

/* 右侧操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  flex-shrink: 0;
}

.action-btn,
.user-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-3);
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.action-btn:hover,
.user-btn:hover {
  background: var(--color-gray-100);
}

/* 用户头像 */
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.user-name {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* 登录按钮 */
.login-btn {
  padding: var(--spacing-2) var(--spacing-4);
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
}

.login-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 响应式 */
@media (max-width: 1024px) {
  .nav-links {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-container {
    padding: 0 var(--spacing-4);
  }

  .action-btn span,
  .user-name {
    display: none;
  }
}
</style>
