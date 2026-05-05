import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface InboundQuery {
  pageNum: number
  pageSize: number
  inboundNo?: string
  arrivalNo?: string
  orderNo?: string
  warehouseName?: string
  status?: string
  inboundTimeBegin?: string
  inboundTimeEnd?: string
}

export interface InboundDTO {
  id?: number
  arrivalId?: number
  remark?: string
}

export interface InboundItemVO {
  id: number
  inboundId?: number
  arrivalItemId?: number
  orderItemId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  inboundNumber?: number
  sortNumber?: number
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface InboundVO {
  id: number
  inboundNo?: string
  arrivalId?: number
  arrivalNo?: string
  orderId?: number
  orderNo?: string
  warehouseId?: number
  warehouseName?: string
  inboundNumber?: number
  status?: string
  operatorId?: number
  inboundTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
  items?: InboundItemVO[]
  deleted?: number
}

export function getInboundPageApi(params: InboundQuery) {
  return request<PageResult<InboundVO>>({
    url: '/inbound/getInboundPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getInboundByIdApi(id: number) {
  return request<InboundVO>({
    url: `/inbound/getInboundById/${id}`,
    method: 'get',
  })
}

export function addInboundApi(data: InboundDTO) {
  return request<null>({
    url: '/inbound/addInbound',
    method: 'post',
    data,
  })
}

export function cancelInboundApi(data: InboundDTO) {
  return request<null>({
    url: '/inbound/cancelInbound',
    method: 'put',
    data,
  })
}

export function confirmInboundApi(data: InboundDTO) {
  return request<null>({
    url: '/inbound/confirmInbound',
    method: 'put',
    data,
  })
}
