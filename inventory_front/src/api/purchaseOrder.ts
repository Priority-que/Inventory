import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface PurchaseOrderQuery {
  pageNum: number
  pageSize: number
  orderNo?: string
  requestTitle?: string
  supplierName?: string
  purchaseName?: string
  planDateBegin?: string
  planDateEnd?: string
  status?: string
  purchaserId?: number
}

export interface PurchaseOrderItemCreateDTO {
  requestItemId?: number
  unitPrice?: number
  remark?: string
  status?: string
}

export interface PurchaseOrderDTO {
  id?: number
  orderNo?: string
  requestId?: number
  supplierId?: number
  purchaserId?: number
  planDate?: string
  supplierDate?: string
  confirmTime?: string
  totalAmount?: number
  status?: string
  supplierNote?: string
  closeTime?: string
  closeReason?: string
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
  items?: PurchaseOrderItemCreateDTO[]
}

export interface PurchaseOrderVO extends PurchaseOrderDTO {
  id: number
  requestTitle?: string
  supplierName?: string
  purchaserName?: string
}

export interface PurchaseOrderItemDTO {
  id?: number
  unitPrice?: number
  remark?: string
}

export interface PurchaseOrderItemVO extends PurchaseOrderItemDTO {
  id: number
  orderId?: number
  requestItemId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  orderNumber?: number
  lineAmount?: number
  arrivedNumber?: number
  inboundNumber?: number
  sortNumber?: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

export function getPurchaseOrderPageApi(params: PurchaseOrderQuery) {
  return request<PageResult<PurchaseOrderVO>>({
    url: '/purchaseOrder/getPurchaseOrderPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getSupplierPurchaseOrderPageApi(params: PurchaseOrderQuery) {
  return request<PageResult<PurchaseOrderVO>>({
    url: '/purchaseOrder/getSupplierPurchaseOrderPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getPurchaseOrderByIdApi(id: number) {
  return request<PurchaseOrderVO>({
    url: `/purchaseOrder/getPurchaseOrderById/${id}`,
    method: 'get',
  })
}

export function addPurchaseOrderApi(data: PurchaseOrderDTO) {
  return request<null>({
    url: '/purchaseOrder/addPurchaseOrder',
    method: 'post',
    data,
  })
}

export function updatePurchaseOrderApi(data: PurchaseOrderDTO) {
  return request<null>({
    url: '/purchaseOrder/updatePurchaseOrder',
    method: 'put',
    data,
  })
}

export function cancelPurchaseOrderApi(data: PurchaseOrderDTO) {
  return request<null>({
    url: '/purchaseOrder/cancelPurchaseOrder',
    method: 'put',
    data,
  })
}

export function confirmPurchaseOrderApi(data: PurchaseOrderDTO) {
  return request<null>({
    url: '/purchaseOrder/confirmPurchaseOrder',
    method: 'put',
    data,
  })
}

export function closePurchaseOrderApi(data: PurchaseOrderDTO) {
  return request<null>({
    url: '/purchaseOrder/closePurchaseOrder',
    method: 'put',
    data,
  })
}

export function getPurchaseOrderItemsByOrderIdApi(orderId: number) {
  return request<PurchaseOrderItemVO[]>({
    url: `/purchaseOrderItem/getPurchaseOrderItemByOrderId/${orderId}`,
    method: 'get',
  })
}

export function getPurchaseOrderItemByIdApi(id: number) {
  return request<PurchaseOrderItemVO>({
    url: `/purchaseOrderItem/getPurchaseOrderItemById/${id}`,
    method: 'get',
  })
}

export function updatePurchaseOrderItemApi(data: PurchaseOrderItemDTO) {
  return request<null>({
    url: '/purchaseOrderItem/updatePurchaseOrderItem',
    method: 'put',
    data,
  })
}
