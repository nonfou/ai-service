import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('../views/Home.vue')
        },
        {
          path: 'getting-started',
          name: 'GettingStarted',
          component: () => import('../views/GettingStarted.vue')
        },
        {
          path: 'models',
          name: 'Models',
          component: () => import('../views/Models.vue')
        },
        {
          path: 'subscriptions',
          name: 'Subscriptions',
          component: () => import('../views/Subscriptions.vue')
        },
        {
          path: 'tickets',
          name: 'Tickets',
          component: () => import('../views/Tickets.vue')
        },
        {
          path: 'api-keys',
          name: 'ApiKeys',
          component: () => import('../views/ApiKeys.vue')
        },
        {
          path: 'wallet',
          name: 'Wallet',
          component: () => import('../views/Wallet.vue')
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue'),
          children: [
            {
              path: '',
              redirect: '/dashboard/overview'
            },
            {
              path: 'overview',
              name: 'Overview',
              component: () => import('../views/dashboard/Overview.vue')
            },
            {
              path: 'api-key',
              name: 'ApiKey',
              component: () => import('../views/dashboard/ApiKey.vue')
            },
            {
              path: 'usage',
              name: 'Usage',
              component: () => import('../views/dashboard/Usage.vue')
            },
            {
              path: 'logs',
              name: 'Logs',
              component: () => import('../views/dashboard/Logs.vue')
            }
          ]
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    }
  ]
})

// 路由守卫（兼容 token 或 本地缓存的 userInfo）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')

  // 如果已登录（token 或 userInfo 存在），访问 /login 时重定向到 dashboard
  if (to.path === '/login' && (token || userInfo)) {
    next('/dashboard')
    return
  }

  // 需要鉴权的路由：要求 token 或 已存在 userInfo
  if (to.meta.requiresAuth && !(token || userInfo)) {
    next('/login')
    return
  }

  next()
})

export default router
