import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface UserQuery {
  pageNum: number
  pageSize: number
  name?: string
  roleName?: string
  dept?: string
  status?: string
}

export interface UserDTO {
  id?: number
  username?: string
  name?: string
  roleName?: string
  roleId?: number
  phone?: string
  email?: string
  dept?: string
  status?: string
  remark?: string
}

export interface UserStatusDTO {
  id: number
  status: 'ENABLED' | 'DISABLED'
}

export interface UserRoleDTO {
  userId: number
  roleId: number
}

export interface UserVO {
  id: number
  username: string
  name: string
  roleName?: string
  phone?: string
  email?: string
  dept?: string
  status?: string
  lastLoginTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export function getUserPageApi(params: UserQuery) {
  return request<PageResult<UserVO>>({
    url: '/user/getUserPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getUserDetailApi(id: number) {
  return request<UserVO>({
    url: `/user/getUserDetailById/${id}`,
    method: 'get',
  })
}

export function addUserApi(data: UserDTO) {
  return request<null>({
    url: '/user/addUser',
    method: 'post',
    data,
  })
}

export function updateUserApi(data: UserDTO) {
  return request<null>({
    url: '/user/updateUser',
    method: 'put',
    data,
  })
}

export function updateUserStatusApi(data: UserStatusDTO) {
  return request<null>({
    url: '/user/updateUserStatus',
    method: 'put',
    data,
  })
}

export function resetPasswordApi(id: number) {
  return request<null>({
    url: `/user/resetPassword/${id}`,
    method: 'put',
  })
}

export function updateUserRoleApi(data: UserRoleDTO) {
  return request<null>({
    url: '/user/updateUserRole',
    method: 'put',
    data,
  })
}
