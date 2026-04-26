import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface WarehousePageQuery {
  pageNum: number
  pageSize: number
  code?: string
  name?: string
  address?: string
  managerName?: string
  status?: string
}

export interface WarehouseDTO {
  id?: number
  code?: string
  name?: string
  address?: string
  managerName?: string
  managerPhone?: string
  status?: string
  remark?: string
}

export interface WarehousePageVO extends WarehouseDTO {
  id: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

export function getWarehousePageApi(params: WarehousePageQuery) {
  return request<PageResult<WarehousePageVO>>({
    url: '/warehouse/getWarehousePage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function addWarehouseApi(data: WarehouseDTO) {
  return request<null>({
    url: '/warehouse/addWarehouse',
    method: 'post',
    data,
  })
}

export function updateWarehouseApi(data: WarehouseDTO) {
  return request<null>({
    url: '/warehouse/updateWarehouse',
    method: 'put',
    data,
  })
}

export function deleteWarehouseApi(ids: number[]) {
  return request<null>({
    url: `/warehouse/deleteWarehouse/${ids.join(',')}`,
    method: 'delete',
  })
}
