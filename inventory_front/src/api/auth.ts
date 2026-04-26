import { request } from '@/utils/request'
import type { ChangePasswordParams, CurrentUser, LoginParams, LoginResult } from '@/types/auth'

export function loginApi(data: LoginParams) {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data,
  })
}

export function logoutApi() {
  return request<null>({
    url: '/auth/logout',
    method: 'post',
  })
}

export function getCurrentUserApi() {
  return request<CurrentUser>({
    url: '/auth/me',
    method: 'get',
  })
}

export function changePasswordApi(data: ChangePasswordParams) {
  return request<null>({
    url: '/auth/password',
    method: 'put',
    data,
  })
}
