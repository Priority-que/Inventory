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
      ],
    },
    {
      title: '系统与智能',
      items: [
        { title: 'AI 智能助手', path: '/admin/agent', icon: 'ChatDotRound' },
        { title: '知识库管理', path: '/admin/knowledge', icon: 'Reading' },
        { title: '操作日志', path: '/admin/log', icon: 'Document' },
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
  background: #ffffff;
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
  font-weight: 700;
  background: #ffffff;
  border: 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  text-align: left;
}

.layout-logo.el-button:hover,
.layout-logo.el-button:focus {
  color: var(--primary-hover);
  background: #ffffff;
}

.layout-logo.el-button :deep(> span) {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: var(--primary-color);
  border-radius: 6px;
  font-size: 18px;
}

.menu-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.layout-menu {
  border-right: 0;
}

.layout-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 2px 12px;
  padding: 0 14px !important;
  border-radius: 6px;
  font-size: 14px;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.layout-menu :deep(.el-menu-item .el-icon) {
  width: 18px;
  margin-right: 10px;
  font-size: 17px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  color: var(--primary-color);
  background: #eff6ff;
  font-weight: 600;
}

.layout-menu :deep(.el-menu-item:hover) {
  color: var(--primary-color);
  background: var(--app-bg);
}

.layout-menu :deep(.el-menu-item-group__title) {
  padding: 16px 24px 8px;
  line-height: 1;
}

.menu-group-title {
  color: #9ca3af;
  font-size: 12px;
  font-weight: 700;
}

.layout-user {
  padding: 14px 12px 18px;
  border-top: 1px solid var(--border-color);
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
  border-radius: 6px;
  cursor: pointer;
}

.user-button.el-button:hover,
.user-button.el-button:focus {
  color: #374151;
  border-color: #bfdbfe;
  background: #f8fbff;
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
  border-radius: 6px;
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
  padding: 32px 40px;
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
