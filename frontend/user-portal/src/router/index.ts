import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/MainLayout.vue'
import { useUserStore } from '../stores/user'

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
          component: () => import('../views/Home.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'getting-started',
          name: 'GettingStarted',
          component: () => import('../views/GettingStarted.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'models',
          name: 'Models',
          component: () => import('../views/Models.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'subscriptions',
          name: 'Subscriptions',
          component: () => import('../views/Subscriptions.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'tickets',
          name: 'Tickets',
          component: () => import('../views/Tickets.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'api-keys',
          name: 'ApiKeys',
          component: () => import('../views/ApiKeys.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'wallet',
          name: 'Wallet',
          component: () => import('../views/Wallet.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: '',
              redirect: '/dashboard/overview'
            },
            {
              path: 'overview',
              name: 'Overview',
              component: () => import('../views/dashboard/Overview.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: 'api-key',
              name: 'ApiKey',
              component: () => import('../views/dashboard/ApiKey.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: 'usage',
              name: 'Usage',
              component: () => import('../views/dashboard/Usage.vue'),
              meta: { requiresAuth: true }
            },
            {
              path: 'logs',
              name: 'Logs',
              component: () => import('../views/dashboard/Logs.vue'),
              meta: { requiresAuth: true }
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

// 路由守卫 - 改为异步守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // ✅ 首次加载或刷新页面时,检查登录状态
  // from.name === undefined 表示是首次进入应用或刷新页面
  if (from.name === undefined && !userStore.isLoggedIn) {
    await userStore.checkLoginStatus()
  }

  // 已登录用户访问登录页,重定向到首页
  if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }

  // 需要认证的路由,检查登录状态
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  next()
})

export default router
