import { request } from '@/utils/request'
import { cleanParams } from '@/utils/params'
import type { PageResult } from '@/types/api'

export interface OperLogPageQuery {
  pageNum: number
  pageSize: number
  logType?: string
  operatorName?: string
  moduleName?: string
  operationType?: string
  operatorId?: number
  successFlag?: number
  beginTime?: string
  endTime?: string
}

export interface OperLogVO {
  id: number
  logType?: string
  moduleName?: string
  bizType?: string
  bizId?: number
  operationType?: string
  operationDesc?: string
  operatorId?: number
  operatorName?: string
  requestUri?: string
  requestMethod?: string
  ipAddress?: string
  successFlag?: number
  errorMessage?: string
  operateTime?: string
  createBy?: string
  createTime?: string
}

export function getOperLogPageApi(params: OperLogPageQuery) {
  return request<PageResult<OperLogVO>>({
    url: '/log/getOperLogPage',
    method: 'get',
    params: cleanParams({ ...params }),
  })
}

export function getOperLogDetailApi(id: number) {
  return request<OperLogVO>({
    url: `/log/getOperLogById/${id}`,
    method: 'get',
  })
}
