import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface InventoryPageQuery {
  pageNum: number
  pageSize: number
  materialCode?: string
  materialName?: string
  warehouseName?: string
  stockStatus?: string
}

export interface InventoryPageVO {
  id: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  warehouseId?: number
  warehouseName?: string
  currentNumber?: number
  safetyNumber?: number
  upperNumber?: number
  stockStatus?: string
  lastInboundTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export function getInventoryPageApi(params: InventoryPageQuery) {
  return request<PageResult<InventoryPageVO>>({
    url: '/inventory/getInventoryPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}