<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { roleOptions } from '@/constants/business'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

interface MenuItem {
  title: string
  path: string
  icon: string
}

interface MenuGroup {
  title: string
  items: MenuItem[]
}

const homeMenu: MenuItem = {
  title: '工作台',
  path: '/home',
  icon: 'DataBoard',
}

const roleMenuMap: Record<string, MenuGroup[]> = {
  ADMIN: [
    {
      title: '核心业务',
      items: [
        { title: '用户管理', path: '/admin/user', icon: 'UserFilled' },
        { title: '物料管理', path: '/admin/material', icon: 'Box' },
        { title: '仓库管理', path: '/admin/warehouse', icon: 'House' },
        { title: '供应商管理', path: '/admin/supplier', icon: 'Connection' },
        { title: '库存台账', path: '/admin/inventory', icon: 'Box' },
      ],
    },
    {
      title: '系统与智能',
      items: [
        { title: 'AI 智能助手', path: '/assistant', icon: 'ChatDotRound' },
        { title: '知识库管理', path: '/admin/knowledge', icon: 'Reading' },
        { title: '操作日志', path: '/admin/log', icon: 'Document' },
      ],
    },
  ],
  PURCHASER: [
    {
      title: '采购执行',
      items: [
        { title: '采购申请', path: '/purchaser/request', icon: 'DocumentAdd' },
        { title: '采购订单', path: '/purchaser/order', icon: 'Tickets' },
        { title: '到货跟踪', path: '/purchaser/arrival', icon: 'Van' },
        { title: '入库跟踪', path: '/purchaser/inbound', icon: 'TakeawayBox' },
      ],
    },
    {
      title: '主数据查询',
      items: [
        { title: '供应商查询', path: '/purchaser/supplier', icon: 'Connection' },
        { title: '物料查询', path: '/purchaser/material', icon: 'Box' },
        { title: '库存台账', path: '/purchaser/inventory', icon: 'Box' },
      ],
    },
    {
      title: '智能分析',
      items: [
        { title: 'AI 智能助手', path: '/assistant', icon: 'ChatDotRound' },
      ],
    },
  ],
  PURCHASE_MANAGER: [
    {
      title: '审批中心',
      items: [{ title: '采购审批', path: '/manager/approval', icon: 'Operation' }],
    },
    {
      title: '常用功能',
      items: [{ title: 'AI 智能助手', path: '/assistant', icon: 'ChatDotRound' }],
    },
    {
      title: '业务查询',
      items: [
        { title: '库存台账', path: '/manager/inventory', icon: 'Box' },
      ],
    },
  ],
  WAREHOUSE: [
    {
      title: '仓配作业',
      items: [
        { title: '到货管理', path: '/warehouse/arrival', icon: 'Van' },
        { title: '入库管理', path: '/warehouse/inbound', icon: 'TakeawayBox' },
        { title: '采购订单查看', path: '/warehouse/order', icon: 'Tickets' },
      ],
    },
    {
      title: '业务查询',
      items: [
        { title: '物料查询', path: '/warehouse/material', icon: 'Box' },
        { title: '仓库查询', path: '/warehouse/warehouse', icon: 'House' },
        { title: '库存台账', path: '/warehouse/inventory', icon: 'Box' },
      ],
    },
    {
      title: '智能诊断',
      items: [
        { title: 'AI 智能助手', path: '/assistant', icon: 'ChatDotRound' },
      ],
    },
  ],
  SUPPLIER: [
    {
      title: '供应商中心',
      items: [
        { title: '订单确认', path: '/supplier/order', icon: 'Tickets' },
        { title: '资料维护', path: '/supplier/profile', icon: 'Connection' },
        { title: 'AI 智能助手', path: '/assistant', icon: 'ChatDotRound' },
      ],
    },
  ],
}

const currentRole = computed(() => authStore.user?.roleCodes?.[0] || 'ADMIN')
const currentRoleName = computed(() => {
  return roleOptions.find((item) => item.code === currentRole.value)?.label || currentRole.value
})
const menuGroups = computed(() => roleMenuMap[currentRole.value] || [])

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  })

  await authStore.logout()
  router.replace('/login')
}

async function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'logout') {
    await handleLogout()
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside class="layout-aside" width="220px">
      <el-button class="layout-logo" text @click="router.push('/home')">
        <span class="logo-mark">
          <el-icon><DataAnalysis /></el-icon>
        </span>
        <span class="logo-title">库存管理系统</span>
      </el-button>

      <div class="menu-scroll">
        <el-menu
          class="layout-menu"
          :default-active="route.path"
          router
          background-color="#ffffff"
          text-color="#6b7280"
          active-text-color="#2563eb"
        >
          <el-menu-item :index="homeMenu.path">
            <el-icon>
              <component :is="homeMenu.icon" />
            </el-icon>
            <span>{{ homeMenu.title }}</span>
          </el-menu-item>

          <el-menu-item-group v-for="group in menuGroups" :key="group.title">
            <template #title>
              <span class="menu-group-title">{{ group.title }}</span>
            </template>

            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </div>

      <el-dropdown class="layout-user" trigger="click" placement="top-start" @command="handleUserCommand">
        <el-button class="user-button">
          <span class="user-avatar">
            <el-icon><User /></el-icon>
          </span>
          <span class="user-meta">
            <span class="user-name">{{ authStore.username }}</span>
            <span class="role-badge">{{ currentRoleName }}</span>
          </span>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </el-button>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              <span>我的信息</span>
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-aside>

    <el-container class="layout-content">
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: var(--app-bg);
}

.layout-aside {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f9fbfe 0%, #ffffff 240px);
  border-right: 1px solid var(--border-color);
}

.layout-logo.el-button {
  width: 100%;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 22px;
  color: var(--primary-color);
  font-size: 18px;
  font-weight: 600;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  text-align: left;
}

.layout-logo.el-button:hover,
.layout-logo.el-button:focus {
  color: var(--primary-hover);
  background: transparent;
}

.layout-logo.el-button :deep(> span) {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.logo-mark {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: var(--primary-color);
  border-radius: 10px;
  font-size: 18px;
  box-shadow: 0 8px 20px rgba(15, 98, 254, 0.16);
}

.menu-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 18px 0;
}

.layout-menu {
  border-right: 0;
}

.layout-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 3px 12px;
  padding: 0 14px !important;
  border: 1px solid transparent;
  border-radius: 10px;
  font-size: 14px;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.layout-menu :deep(.el-menu-item .el-icon) {
  width: 18px;
  margin-right: 10px;
  font-size: 17px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  color: var(--primary-color);
  background: #eef5ff;
  border-color: #d7e6ff;
  font-weight: 600;
}

.layout-menu :deep(.el-menu-item:hover) {
  color: var(--primary-color);
  background: #f4f8fd;
  border-color: #e6edf6;
}

.layout-menu :deep(.el-menu-item-group__title) {
  padding: 16px 24px 8px;
  line-height: 1;
}

.menu-group-title {
  color: #8a97aa;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.layout-user {
  padding: 14px 12px 18px;
  border-top: 1px solid var(--border-color);
  background: rgba(249, 251, 254, 0.92);
}

.user-button.el-button {
  width: 100%;
  min-height: 52px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  color: #374151;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
}

.user-button.el-button:hover,
.user-button.el-button:focus {
  color: #374151;
  border-color: #d7e6ff;
  background: #f7fbff;
}

.user-button.el-button :deep(> span) {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  background: #eff6ff;
  border-radius: 8px;
}

.user-meta {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.user-name {
  max-width: 112px;
  overflow: hidden;
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  color: var(--text-secondary);
  font-size: 12px;
}

.layout-content {
  min-width: 0;
}

.layout-main {
  min-width: 0;
  padding: 28px 32px;
  background: var(--app-bg);
}

@media (max-width: 960px) {
  .layout-aside {
    width: 72px !important;
  }

  .layout-logo {
    justify-content: center;
    padding: 0;
  }

  .layout-logo.el-button :deep(> span),
  .user-button.el-button :deep(> span) {
    justify-content: center;
  }

  .logo-title,
  .layout-menu :deep(.el-menu-item span),
  .layout-menu :deep(.el-menu-item-group__title),
  .user-meta,
  .user-arrow {
    display: none;
  }

  .layout-menu :deep(.el-menu-item) {
    justify-content: center;
    margin: 4px 10px;
    padding: 0 !important;
  }

  .layout-menu :deep(.el-menu-item .el-icon) {
    margin-right: 0;
  }

  .user-button {
    justify-content: center;
    padding: 8px;
  }

  .layout-main {
    padding: 24px;
  }
}

@media (max-width: 640px) {
  .layout-main {
    padding: 18px;
  }
}
</style>
