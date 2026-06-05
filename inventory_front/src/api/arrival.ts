import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface ArrivalQuery {
  pageNum: number
  pageSize: number
  arrivalNo?: string
  orderNo?: string
  warehouseName?: string
  arrivalDateBegin?: string
  arrivalDateEnd?: string
  status?: string
  pendingInboundOnly?: boolean
}

export interface ArrivalItemDTO {
  orderItemId?: number
  arrivalNumber?: number
  qualifiedNumber?: number
  unqualifiedNumber?: number
  abnormalNote?: string
  remark?: string
}

export interface ArrivalDTO {
  id?: number
  orderId?: number
  warehouseId?: number
  arrivalDate?: string
  remark?: string
  items?: ArrivalItemDTO[]
  deleted?: number
}

export interface ArrivalItemVO {
  id: number
  arrivalId?: number
  orderItemId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  arrivalNumber?: number
  qualifiedNumber?: number
  unqualifiedNumber?: number
  abnormalNote?: string
  sortNumber?: number
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface ArrivalVO {
  id: number
  arrivalNo?: string
  orderId?: number
  orderNo?: string
  warehouseId?: number
  warehouseName?: string
  arrivalDate?: string
  arrivalNumber?: number
  qualifiedNumber?: number
  unqualifiedNumber?: number
  status?: string
  abnormalNote?: string
  operatorId?: number
  remark?: string
  createTime?: string
  updateTime?: string
  items?: ArrivalItemVO[]
  deleted?: number
}

export function getArrivalPageApi(params: ArrivalQuery) {
  return request<PageResult<ArrivalVO>>({
    url: '/arrival/getArrivalPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getArrivalByIdApi(arrivalId: number) {
  return request<ArrivalVO>({
    url: `/arrival/getArrivalById/${arrivalId}`,
    method: 'get',
  })
}

export function addArrivalApi(data: ArrivalDTO) {
  return request<null>({
    url: '/arrival/addArrival',
    method: 'post',
    data,
  })
}
