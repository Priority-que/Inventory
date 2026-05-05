import { getOptionLabel, getOptionType, riskLevelOptions } from '@/constants/business'

export interface WorkflowWarningItem {
  riskLevel?: string
  bizType?: string
  bizId?: number
  bizNo?: string
  problem?: string
  reason?: string
  suggestOwner?: string
  suggestAction?: string
}

export function asWorkflowRecord(value: unknown) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }

  return null
}

export function getWorkflowString(data: unknown, key: string) {
  const record = asWorkflowRecord(data)
  const value = record?.[key]

  if (typeof value === 'string' || typeof value === 'number') {
    return String(value)
  }

  return ''
}

export function getWorkflowNumber(data: unknown, key: string) {
  const record = asWorkflowRecord(data)
  const value = record?.[key]

  if (typeof value === 'number') {
    return value
  }

  if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
    return Number(value)
  }

  return undefined
}

export function getWorkflowEvidence(data: unknown) {
  const record = asWorkflowRecord(data)
  const evidence = record?.evidence

  if (!Array.isArray(evidence)) {
    return []
  }

  return evidence.map((item) => String(item))
}

export function getWorkflowWarnings(data: unknown) {
  const record = asWorkflowRecord(data)
  const items = Array.isArray(record?.items) ? record.items : record?.topItems

  if (!Array.isArray(items)) {
    return []
  }

  return items.map((item) => asWorkflowRecord(item) || {}) as WorkflowWarningItem[]
}

export function getWorkflowNextAction(data: unknown) {
  const record = asWorkflowRecord(data)
  const nextAction = asWorkflowRecord(record?.nextAction)

  return (
    getWorkflowString(nextAction, 'actionText') ||
    getWorkflowString(record, 'suggestAction') ||
    getWorkflowString(record, 'nextAction')
  )
}

export function getWorkflowResponsibility(data: unknown) {
  const record = asWorkflowRecord(data)
  const responsibility = asWorkflowRecord(record?.responsibility)

  return getWorkflowString(responsibility, 'ownerRoleName') || getWorkflowString(record, 'suggestOwner')
}

export function getWorkflowIntentLabel(intent?: string) {
  const labelMap: Record<string, string> = {
    ORDER_DIAGNOSIS: '订单诊断',
    WARNING_SCAN: '风险预警',
    SUPPLIER_SCORE: '供应商评分',
    KNOWLEDGE_QA: '知识问答',
    UNKNOWN: '普通对话',
  }

  return intent ? labelMap[intent] || intent : '普通对话'
}

export function getRiskLabel(level?: string) {
  return getOptionLabel(riskLevelOptions, level)
}

export function getRiskType(level?: string) {
  return getOptionType(riskLevelOptions, level)
}
