import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeStoredUser, removeToken } from '@/utils/auth'
import type { ApiResult } from '@/types/api'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: Number(import.meta.env.VITE_API_TIMEOUT || 30000),
})

let authErrorNotified = false

function isLoginRequest(url?: string) {
  return Boolean(url?.includes('/auth/login'))
}

async function clearClientAuth() {
  removeToken()
  removeStoredUser()
}

async function redirectToLogin(message: string) {
  await clearClientAuth()

  if (!authErrorNotified) {
    ElMessage.error(message)
    authErrorNotified = true
  }

  const currentPath = router.currentRoute.value.fullPath
  await router.replace({
    path: '/login',
    query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : undefined,
  }).catch(() => undefined)
}

service.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

service.interceptors.response.use(
  async (response: AxiosResponse<ApiResult>) => {
    const result = response.data

    if (result.code === 200) {
      authErrorNotified = false
      return response
    }

    if (result.code === 401 || result.code === 403) {
      const message = result.code === 403 ? '无权限访问，请重新登录或切换账号' : result.msg || '登录已失效，请重新登录'
      if (!isLoginRequest(response.config.url)) {
        await redirectToLogin(message)
        return Promise.reject(new Error(message))
      }
    }

    ElMessage.error(result.msg || '操作失败')
    return Promise.reject(new Error(result.msg || '操作失败'))
  },
  async (error) => {
    const status = error.response?.status

    if (status === 401 || status === 403) {
      const message = status === 403
        ? '无权限访问，请重新登录或切换账号'
        : error.response?.data?.msg || '登录已失效，请重新登录'
      if (!isLoginRequest(error.config?.url)) {
        await redirectToLogin(message)
        return Promise.reject(error)
      }
    }

    ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    return Promise.reject(error)
  },
)

export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const response = await service.request<ApiResult<T>>(config)
  return response.data.data
}
