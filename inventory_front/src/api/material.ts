import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface MaterialPageQuery {
  pageNum: number
  pageSize: number
  code?: string
  name?: string
  specification?: string
  safetyNumber?: number
  safetyNumberBegin?: number
  upperNumber?: number
  upperNumberBegin?: number
  status?: string
}

export interface MaterialDTO {
  id?: number
  code?: string
  name?: string
  specification?: string
  unit?: string
  categoryName?: string
  safetyNumber?: number
  upperNumber?: number
  status?: string
  remark?: string
}

export interface MaterialPageVO extends MaterialDTO {
  id: number
  createTime?: string
  updateTime?: string
  deleted?: number
}

export function getMaterialPageApi(params: MaterialPageQuery) {
  return request<PageResult<MaterialPageVO>>({
    url: '/material/getMaterialPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function addMaterialApi(data: MaterialDTO) {
  return request<null>({
    url: '/material/addMaterial',
    method: 'post',
    data,
  })
}

export function updateMaterialApi(data: MaterialDTO) {
  return request<null>({
    url: '/material/updateMaterial',
    method: 'put',
    data,
  })
}

export function deleteMaterialApi(ids: number[]) {
  return request<null>({
    url: `/material/deleteMaterialByIds/${ids.join(',')}`,
    method: 'delete',
  })
}
