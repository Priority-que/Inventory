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
          path:'admin/inventory',
          name:'AdminInventory',
          component: () => import('@/view/warehouse/inventory/index.vue'),
          meta: {
            title: '库存台账', requiresAuth: true,roles: ['ADMIN'],icon: 'Box'},
        },
        {
          path: 'admin/agent',
          name: 'AdminAgentRisk',
          component: () => import('@/view/admin/agent/index.vue'),
          meta: { title: 'AI 智能助手', requiresAuth: true, roles: ['ADMIN'], icon: 'ChatDotRound' },
        },
        {
          path: 'assistant',
          name: 'Assistant',
          component: () => import('@/view/admin/agent/index.vue'),
          meta: {
            title: 'AI 智能助手',
            requiresAuth: true,
            roles: ['ADMIN', 'PURCHASER', 'PURCHASE_MANAGER', 'WAREHOUSE', 'SUPPLIER'],
            icon: 'ChatDotRound',
          },
        },
        {
          path: 'manager/approval',
          name: 'PurchaseManagerApproval',
          component: () => import('@/view/manager/approval/index.vue'),
          meta: {
            title: '采购审批',
            requiresAuth: true,
            roles: ['PURCHASE_MANAGER'],
            icon: 'Operation',
          },
        },
        {
          path: 'purchaser/request',
          name: 'PurchaserRequest',
          component: () => import('@/view/purchaser/request/index.vue'),
          meta: {
            title: '采购申请',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'DocumentAdd',
          },
        },
        {
          path: 'purchaser/order',
          name: 'PurchaserOrder',
          component: () => import('@/view/purchaser/order/index.vue'),
          meta: {
            title: '采购订单',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'Tickets',
          },
        },
        {
          path: 'purchaser/supplier',
          name: 'PurchaserSupplier',
          component: () => import('@/view/admin/supplier/index.vue'),
          meta: {
            title: '供应商查询',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'Connection',
          },
        },
        {
          path: 'purchaser/material',
          name: 'PurchaserMaterial',
          component: () => import('@/view/admin/material/index.vue'),
          meta: {
            title: '物料查询',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'Box',
          },
        },
        {
          path: 'purchaser/arrival',
          name: 'PurchaserArrival',
          component: () => import('@/view/warehouse/arrival/index.vue'),
          meta: {
            title: '到货跟踪',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'Van',
          },
        },
        {
          path: 'purchaser/inbound',
          name: 'PurchaserInbound',
          component: () => import('@/view/warehouse/inbound/index.vue'),
          meta: {
            title: '入库跟踪',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'TakeawayBox',
          },
        },
        {
          path: 'purchaser/order-diagnosis',
          name: 'PurchaserOrderDiagnosis',
          redirect: '/assistant',
          meta: {
            title: '订单诊断',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'Search',
          },
        },
        {
          path: 'purchaser/supplier-score',
          name: 'PurchaserSupplierScore',
          redirect: '/assistant',
          meta: {
            title: '供应商评分',
            requiresAuth: true,
            roles: ['PURCHASER'],
            icon: 'TrendCharts',
          },
        },
        {
          path: 'warehouse/arrival',
          name: 'WarehouseArrival',
          component: () => import('@/view/warehouse/arrival/index.vue'),
          meta: {
            title: '到货管理',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Van',
          },
        },
        {
          path: 'warehouse/inbound',
          name: 'WarehouseInbound',
          component: () => import('@/view/warehouse/inbound/index.vue'),
          meta: {
            title: '入库管理',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'TakeawayBox',
          },
        },
        {
          path: 'warehouse/inventory',
          name: 'WarehouseInventory',
          component: () => import('@/view/warehouse/inventory/index.vue'),
          meta: {
            title: '库存台账',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Box',
          },
        },
        {
          path: 'warehouse/order',
          name: 'WarehouseOrder',
          component: () => import('@/view/purchaser/order/index.vue'),
          meta: {
            title: '采购订单查看',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Tickets',
          },
        },
        {
          path: 'warehouse/material',
          name: 'WarehouseMaterial',
          component: () => import('@/view/admin/material/index.vue'),
          meta: {
            title: '物料查询',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Box',
          },
        },
        {
          path: 'warehouse/warehouse',
          name: 'WarehouseWarehouse',
          component: () => import('@/view/admin/warehouse/index.vue'),
          meta: {
            title: '仓库查询',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'House',
          },
        },
        {
          path: 'warehouse/order-diagnosis',
          name: 'WarehouseOrderDiagnosis',
          redirect: '/assistant',
          meta: {
            title: '订单诊断',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Search',
          },
        },
        {
          path: 'warehouse/warning',
          name: 'WarehouseWarning',
          redirect: '/assistant',
          meta: {
            title: '风险预警',
            requiresAuth: true,
            roles: ['WAREHOUSE'],
            icon: 'Warning',
          },
        },
        {
          path: 'supplier/profile',
          name: 'SupplierProfile',
          component: () => import('@/view/admin/supplier/index.vue'),
          meta: {
            title: '供应商资料维护',
            requiresAuth: true,
            roles: ['SUPPLIER'],
            icon: 'Connection',
          },
        },
        {
          path: 'supplier/order',
          name: 'SupplierOrderConfirm',
          component: () => import('@/view/purchaser/order/index.vue'),
          meta: {
            title: '订单确认',
            requiresAuth: true,
            roles: ['SUPPLIER'],
            icon: 'Tickets',
          },
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
