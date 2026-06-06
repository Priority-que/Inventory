import { request } from '@/utils/request'

export interface WorkflowAgentRequest {
  message?: string
  threadId?: string
}

export interface WorkflowAgentResponse {
  sessionId?: number
  threadId?: string
  intent?: string
  answer?: string
  data?: Record<string, unknown> | null
}

export interface AgentSession {
  id?: number
  sessionNo?: string
  threadId?: string
  userId?: number
  title?: string
  agentType?: string
  currentIntent?: string
  status?: string
  lastMessageTime?: string
  createTime?: string
}

export interface AgentMessage {
  id?: number
  sessionId?: number
  threadId?: string
  messageRole?: string
  messageType?: string
  content?: string
  createTime?: string
}

export interface AgentSessionDetail {
  session?: AgentSession
  messages?: AgentMessage[]
}

export interface WarningScanRequest {
  days?: number
  threadId?: string
}

export interface WarningItemVO {
  riskLevel?: string
  bizType?: string
  bizId?: number
  bizNo?: string
  problem?: string
  reason?: string
  suggestOwner?: string
  suggestAction?: string
}

export interface WarningScanVO {
  summary?: string
  items?: WarningItemVO[]
  aiSummary?: string | null
}

export interface SupplierScoreRequest {
  supplierId?: number
  days?: number
  threadId?: string
}

export interface SupplierScoreVO {
  supplierId?: number
  supplierName?: string
  score?: number
  level?: string
  confirmRate?: string
  arrivalCompletionRate?: string
  inboundCompletionRate?: string
  abnormalArrivalRate?: string
  analysis?: string
  suggestion?: string
}

export function workflowExecuteApi(data: WorkflowAgentRequest) {
  return request<WorkflowAgentResponse>({
    url: '/agent/workflow/execute',
    method: 'post',
    data,
  })
}

export function agentSessionHistoryApi() {
  return request<AgentSession[]>({
    url: '/agent/session/history',
    method: 'get',
  })
}

export function agentSessionDetailApi(threadId: string) {
  return request<AgentSessionDetail>({
    url: `/agent/session/history/${threadId}`,
    method: 'get',
  })
}

export function warningScanApi(data: WarningScanRequest) {
  return request<WarningScanVO>({
    url: '/agent/warning/scan',
    method: 'post',
    data,
  })
}

export function supplierScoreApi(data: SupplierScoreRequest) {
  return request<SupplierScoreVO>({
    url: '/agent/supplier/score',
    method: 'post',
    data,
  })
}
