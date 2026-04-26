import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/view/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/BasicLayout.vue'),
      redirect: '/home',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'home',
          name: 'HomeView',
          component: () => import('@/view/home/index.vue'),
          meta: { title: '工作台', requiresAuth: true, icon: 'DataBoard' },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/view/profile/index.vue'),
          meta: { title: '我的信息', requiresAuth: true, icon: 'User' },
        },
        {
          path: 'admin/user',
          name: 'AdminUser',
          component: () => import('@/view/admin/user/index.vue'),
          meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN'], icon: 'UserFilled' },
        },
        {
          path: 'admin/material',
          name: 'AdminMaterial',
          component: () => import('@/view/admin/material/index.vue'),
          meta: { title: '物料管理', requiresAuth: true, roles: ['ADMIN'], icon: 'Box' },
        },
        {
          path: 'admin/warehouse',
          name: 'AdminWarehouse',
          component: () => import('@/view/admin/warehouse/index.vue'),
          meta: { title: '仓库管理', requiresAuth: true, roles: ['ADMIN'], icon: 'House' },
        },
        {
          path: 'admin/supplier',
          name: 'AdminSupplier',
          component: () => import('@/view/admin/supplier/index.vue'),
          meta: { title: '供应商管理', requiresAuth: true, roles: ['ADMIN'], icon: 'Connection' },
        },
        {
          path: 'admin/agent',
          name: 'AdminAgentRisk',
          component: () => import('@/view/admin/agent/index.vue'),
          meta: { title: 'AI 智能助手', requiresAuth: true, roles: ['ADMIN'], icon: 'ChatDotRound' },
        },
        {
          path: 'admin/knowledge',
          name: 'AdminKnowledge',
          component: () => import('@/view/admin/knowledge/index.vue'),
          meta: { title: '知识库管理', requiresAuth: true, roles: ['ADMIN'], icon: 'Reading' },
        },
        {
          path: 'admin/log',
          name: 'AdminLog',
          component: () => import('@/view/admin/log/index.vue'),
          meta: { title: '操作日志', requiresAuth: true, roles: ['ADMIN'], icon: 'Document' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/home',
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.path === '/login' && authStore.token) {
    return '/home'
  }

  if (to.meta.requiresAuth && authStore.token && !authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      authStore.clearAuth()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }
  }

  const routeRoles = to.meta.roles as string[] | undefined
  if (routeRoles?.length) {
    const userRoles = authStore.user?.roleCodes || []
    const hasRole = routeRoles.some((role) => userRoles.includes(role))

    if (!hasRole) {
      return '/home'
    }
  }

  document.title = `${String(to.meta.title || '首页')} - ${import.meta.env.VITE_APP_TITLE}`
  return true
})

export default router
