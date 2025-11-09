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
      redirect: '/overview',
      children: [
        {
          path: '/overview',
          name: 'Overview',
          component: () => import('../views/Overview.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/users',
          name: 'Users',
          component: () => import('../views/Users.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/models',
          name: 'Models',
          component: () => import('../views/Models.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/plans',
          name: 'Plans',
          component: () => import('../views/Plans.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/orders',
          name: 'Orders',
          component: () => import('../views/Orders.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/tickets',
          name: 'Tickets',
          component: () => import('../views/Tickets.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/admin/backend-accounts',
          name: 'BackendAccounts',
          component: () => import('../views/admin/BackendAccounts.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/admin/quotas',
          name: 'QuotaManagement',
          component: () => import('../views/admin/QuotaManagement.vue'),
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
