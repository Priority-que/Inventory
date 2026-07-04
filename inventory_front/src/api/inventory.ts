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

export interface InventoryLogPageQuery {
  pageNum: number
  pageSize: number
  inventoryId?: number
  materialId?: number
  warehouseId?: number
  bizType?: string
  beginTime?: string
  endTime?: string
}

export interface InventoryLogPageVO {
  id: number
  logNo?: string
  inventoryId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  warehouseId?: number
  warehouseName?: string
  bizType?: string
  bizId?: number
  beforeNumber?: number
  changeNumber?: number
  afterNumber?: number
  operatorId?: number
  operatorName?: string
  remark?: string
  operateTime?: string
}

export interface InventoryAdjustDTO {
  inventoryId?: number
  changeNumber?: number
  reason?: string
}

export function getInventoryPageApi(params: InventoryPageQuery) {
  return request<PageResult<InventoryPageVO>>({
    url: '/inventory/getInventoryPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getInventoryLogPageApi(params: InventoryLogPageQuery) {
  return request<PageResult<InventoryLogPageVO>>({
    url: '/inventoryLog/getInventoryLogPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function adjustInventoryApi(data: InventoryAdjustDTO) {
  return request<null>({
    url: '/inventory/adjust',
    method: 'post',
    data,
  })
}
