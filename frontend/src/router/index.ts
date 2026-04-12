import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('../views/Layout.vue'),
      meta: { requiresAuth: true },
      redirect: '/admin/token-dashboard',
      children: [
        {
          path: '/admin/token-dashboard',
          name: 'TokenDashboard',
          component: () => import('../views/admin/TokenDashboard.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/admin/token-usage',
          name: 'TokenUsage',
          component: () => import('../views/admin/TokenUsage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/admin/api-keys',
          name: 'ApiKeyManagement',
          component: () => import('../views/admin/ApiKeyManagement.vue'),
          meta: { requiresAuth: true }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('adminToken')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
