import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface PurchaseRequestQuery {
  pageNum: number
  pageSize: number
  requestNo?: string
  title?: string
  dept?: string
  submitTimeBegin?: string
  submitTimeEnd?: string
  status?: string
}

export interface PurchaseRequestDTO {
  id?: number
  requestNo?: string
  title?: string
  applicantId?: number
  dept?: string
  expectedDate?: string
  submitTime?: string
  reviewUserId?: number
  reviewTime?: string
  reviewNote?: string
  status?: string
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface PurchaseRequestPageVO extends PurchaseRequestDTO {
  id: number
  applicantName?: string
  reviewUserName?: string
}

export interface PurchaseRequestVO extends PurchaseRequestDTO {
  id: number
}

export interface PurchaseRequestItemDTO {
  id?: number
  requestId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  requestNumber?: number
  sortNumber?: number
  remark?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface PurchaseRequestItemVO extends PurchaseRequestItemDTO {
  id: number
  requestNo?: string
  requestTitle?: string
}

export interface PurchaseRequestReviewVO {
  id: number
  requestId?: number
  actionType?: string
  fromStatus?: string
  toStatus?: string
  operatorId?: number
  operatorName?: string
  operateNote?: string
  operateTime?: string
}

export function getPurchaseRequestPageApi(params: PurchaseRequestQuery) {
  return request<PageResult<PurchaseRequestPageVO>>({
    url: '/purchaseRequest/getPurchaseRequestPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getPurchaseRequestByIdApi(id: number) {
  return request<PurchaseRequestVO>({
    url: `/purchaseRequest/getPurchaseRequestById/${id}`,
    method: 'get',
  })
}

export function addPurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/addPurchaseRequest',
    method: 'post',
    data,
  })
}

export function updatePurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/updatePurchaseRequest',
    method: 'put',
    data,
  })
}

export function submitPurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/submitPurchaseRequest',
    method: 'put',
    data,
  })
}

export function withdrawPurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/withdrawPurchaseRequest',
    method: 'put',
    data,
  })
}

export function approvePurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/approvePurchaseRequest',
    method: 'put',
    data,
  })
}

export function rejectPurchaseRequestApi(data: PurchaseRequestDTO) {
  return request<null>({
    url: '/purchaseRequest/rejectPurchaseRequest',
    method: 'put',
    data,
  })
}

export function deletePurchaseRequestApi(ids: number[]) {
  return request<null>({
    url: `/purchaseRequest/deletePurchaseRequest/${ids.join(',')}`,
    method: 'delete',
  })
}

export function getPurchaseRequestItemsByRequestIdApi(id: number) {
  return request<PurchaseRequestItemVO[]>({
    url: `/purchaseRequestItem/getPurchaseRequestItemByRequestId/${id}`,
    method: 'get',
  })
}

export function getPurchaseRequestItemByIdApi(id: number) {
  return request<PurchaseRequestItemVO>({
    url: `/purchaseRequestItem/getPurchaseRequestItemById/${id}`,
    method: 'get',
  })
}

export function addPurchaseRequestItemApi(data: PurchaseRequestItemDTO) {
  return request<null>({
    url: '/purchaseRequestItem/addPurchaseRequestItem',
    method: 'post',
    data,
  })
}

export function updatePurchaseRequestItemApi(data: PurchaseRequestItemDTO) {
  return request<null>({
    url: '/purchaseRequestItem/updatePurchaseRequestItem',
    method: 'put',
    data,
  })
}

export function deletePurchaseRequestItemApi(ids: number[]) {
  return request<null>({
    url: `/purchaseRequestItem/deletePurchaseRequestItem/${ids.join(',')}`,
    method: 'delete',
  })
}

export function getPurchaseRequestReviewsByRequestIdApi(requestId: number) {
  return request<PurchaseRequestReviewVO[]>({
    url: `/purchaseRequestReview/getPurchaseRequestReviewByRequestId/${requestId}`,
    method: 'get',
  })
}
