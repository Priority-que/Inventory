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

service.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const result = response.data

    if (result.code === 200) {
      return response
    }

    if (result.code === 401 || result.code === 403) {
      removeToken()
      removeStoredUser()
      router.replace('/login')
    }

    ElMessage.error(result.msg || '操作失败')
    return Promise.reject(new Error(result.msg || '操作失败'))
  },
  (error) => {
    const status = error.response?.status

    if (status === 401 || status === 403) {
      removeToken()
      removeStoredUser()
      router.replace('/login')
    }

    ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    return Promise.reject(error)
  },
)

export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const response = await service.request<ApiResult<T>>(config)
  return response.data.data
}
