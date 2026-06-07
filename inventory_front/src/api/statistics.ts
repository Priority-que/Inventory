import { request } from '@/utils/request'

export interface StatisticsSummaryVO {
  pendingApprovalRequestCount: number
  waitConfirmOrderCount: number
  inProgressOrderCount: number
  pendingInboundArrivalCount: number
  inventoryAlertCount: number
  lowStockCount: number
  overStockCount: number
  abnormalArrivalCount: number
}

export function getStatisticsSummaryApi() {
  return request<StatisticsSummaryVO>({
    url: '/statistics/summary',
    method: 'get',
  })
}