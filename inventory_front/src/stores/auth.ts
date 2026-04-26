import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUserApi, loginApi, logoutApi } from '@/api/auth'
import {
  getStoredUser,
  getToken,
  removeStoredUser,
  removeToken,
  setStoredUser,
  setToken,
} from '@/utils/auth'
import type { CurrentUser, LoginParams } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<CurrentUser | null>(getStoredUser<CurrentUser>())

  const isLogin = computed(() => Boolean(token.value))
  const username = computed(() => user.value?.name || user.value?.username || '用户')

  async function login(params: LoginParams) {
    const result = await loginApi(params)

    token.value = result.token
    setToken(result.token)

    user.value = {
      id: result.userId,
      username: result.username,
      name: result.name,
      roleCodes: result.roleCodes || [],
    }
    setStoredUser(user.value)

    return result
  }

  async function fetchCurrentUser() {
    const result = await getCurrentUserApi()
    user.value = result
    setStoredUser(result)
    return result
  }

  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } finally {
      clearAuth()
    }
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    removeToken()
    removeStoredUser()
  }

  return {
    token,
    user,
    isLogin,
    username,
    login,
    fetchCurrentUser,
    logout,
    clearAuth,
  }
})
