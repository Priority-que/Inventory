import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface SupplierPageQuery {
  pageNum: number
  pageSize: number
  code?: string
  name?: string
  contactName?: string
  contactPhone?: string
  status?: string
}

export interface SupplierDTO {
  id?: number
  userId?: number
  code?: string
  name?: string
  contactName?: string
  contactPhone?: string
  email?: string
  address?: string
  licenseNo?: string
  remark?: string
}

export interface SupplierVO extends SupplierDTO {
  id: number
  fileRound?: number
  status?: string
  submitTime?: string
  reviewTime?: string
  reviewUserId?: number
  reviewNote?: string
  createTime?: string
  updateTime?: string
  deleted?: number
}

export interface SupplierReviewDTO {
  id: number
  reviewNote?: string
}

export interface SupplierFileUploadParams {
  supplierId: number
  fileType: string
  remark?: string
  file: File
}

export function getSupplierPageApi(params: SupplierPageQuery) {
  return request<PageResult<SupplierVO>>({
    url: '/supplier/getSupplierPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function addSupplierApi(data: SupplierDTO) {
  return request<null>({
    url: '/supplier/addSupplier',
    method: 'post',
    data,
  })
}

export function updateSupplierApi(data: SupplierDTO) {
  return request<null>({
    url: '/supplier/updateSupplier',
    method: 'put',
    data,
  })
}

export function deleteSupplierApi(ids: number[]) {
  return request<null>({
    url: `/supplier/deleteSupplier/${ids.join(',')}`,
    method: 'delete',
  })
}

export function uploadSupplierFileApi(data: SupplierFileUploadParams) {
  const formData = new FormData()
  formData.append('supplierId', String(data.supplierId))
  formData.append('fileType', data.fileType)
  formData.append('file', data.file)

  if (data.remark) {
    formData.append('remark', data.remark)
  }

  return request<number>({
    url: '/supplierFile/uploadSupplierFile',
    method: 'post',
    data: formData,
  })
}

export function submitSupplierReviewApi(data: SupplierReviewDTO) {
  return request<null>({
    url: '/supplier/submitReview',
    method: 'put',
    data,
  })
}

export function approveSupplierApi(data: SupplierReviewDTO) {
  return request<null>({
    url: '/supplier/approve',
    method: 'put',
    data,
  })
}

export function rejectSupplierApi(data: SupplierReviewDTO) {
  return request<null>({
    url: '/supplier/reject',
    method: 'put',
    data,
  })
}

export function disableSupplierApi(data: SupplierReviewDTO) {
  return request<null>({
    url: '/supplier/disable',
    method: 'put',
    data,
  })
}
