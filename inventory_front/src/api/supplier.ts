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
  reviewNote?: string
  createTime?: string
  updateTime?: string
  deleted?: number
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
