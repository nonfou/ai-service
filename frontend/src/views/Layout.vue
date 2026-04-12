<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- Logo 区域 -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="logo-text">Copilot</span>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActiveRoute(item.path) }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span class="nav-text">{{ item.title }}</span>
        </router-link>
      </nav>

      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">
            <UserFilled />
          </div>
          <div class="user-details">
            <span class="user-name">{{ adminStore.adminInfo?.username || '管理员' }}</span>
            <span class="user-role">超级管理员</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部导航栏 -->
      <header class="header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-dropdown">
              <div class="user-avatar-small">
                <UserFilled />
              </div>
              <span class="user-name-header">{{ adminStore.adminInfo?.username || '管理员' }}</span>
              <ArrowDown class="dropdown-arrow" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item divided command="logout">
                  <SwitchButton class="dropdown-icon" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Histogram,
  Lock,
  UserFilled,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAdminStore } from '../stores/admin'
import message from '../utils/message'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

// 菜单项配置
const menuItems = [
  { path: '/admin/token-dashboard', title: 'Token 仪表盘', icon: DataAnalysis },
  { path: '/admin/token-usage', title: '使用记录', icon: Histogram },
  { path: '/admin/api-keys', title: 'Copilot API Key', icon: Lock }
]

const isActiveRoute = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const pageTitle = computed(() => {
  const item = menuItems.find(m => isActiveRoute(m.path))
  return item?.title || '管理后台'
})

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      adminStore.logout()
      message.success('已退出登录')
      router.push('/login')
    } catch {
      // 用户取消
    }
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: var(--bg-secondary);
}

/* ==================== 侧边栏样式 ==================== */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, var(--sidebar-bg-dark) 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.5px;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: var(--radius-md);
  color: var(--sidebar-text);
  text-decoration: none;
  transition: all var(--transition-normal);
  font-size: 14px;
  font-weight: 500;
}

.nav-item:hover {
  background-color: var(--sidebar-hover);
  color: var(--sidebar-text-active);
}

.nav-item.active {
  background-color: var(--sidebar-active);
  color: var(--sidebar-text-active);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-text {
  white-space: nowrap;
}

/* 底部用户信息 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: var(--radius-md);
  transition: background-color var(--transition-normal);
}

.user-info:hover {
  background-color: var(--sidebar-hover);
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  color: var(--sidebar-text-active);
  font-size: 14px;
  font-weight: 500;
}

.user-role {
  color: var(--sidebar-text);
  font-size: 12px;
}

/* ==================== 主内容区样式 ==================== */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部导航栏 */
.header {
  height: 64px;
  background-color: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 用户下拉菜单 */
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.user-dropdown:hover {
  background-color: var(--bg-tertiary);
}

.user-avatar-small {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.user-name-header {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.dropdown-arrow {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
}

.dropdown-icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: var(--bg-secondary);
}

/* ==================== Element Plus 下拉菜单覆盖 ==================== */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  font-size: 14px;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
}
</style>
