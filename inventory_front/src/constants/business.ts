export const enabledStatusOptions = [
  { label: '启用', value: 'ENABLED', type: 'success' },
  { label: '禁用', value: 'DISABLED', type: 'info' },
] as const

export const supplierStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '待审核', value: 'PENDING', type: 'warning' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '可合作', value: 'ACTIVE', type: 'success' },
  { label: '停用', value: 'DISABLED', type: 'info' },
] as const

export const roleOptions = [
  { label: '系统管理员', value: 1, code: 'ADMIN' },
  { label: '采购员', value: 2, code: 'PURCHASER' },
  { label: '采购主管', value: 3, code: 'PURCHASE_MANAGER' },
  { label: '仓库岗', value: 4, code: 'WAREHOUSE' },
  { label: '供应商', value: 5, code: 'SUPPLIER' },
] as const

export const fileTypeOptions = [
  { label: '营业执照', value: 'business_license' },
  { label: '银行开户许可证', value: 'bank_license' },
  { label: '合同文件', value: 'contract' },
  { label: '其他', value: 'other' },
] as const

export const intentOptions = [
  { label: '通用知识', value: 'COMMON' },
  { label: '订单诊断', value: 'ORDER_DIAGNOSIS' },
  { label: '风险预警', value: 'WARNING_SCAN' },
  { label: '供应商评分', value: 'SUPPLIER_SCORE' },
  { label: '知识问答', value: 'KNOWLEDGE_QA' },
] as const

export function getOptionLabel(
  options: readonly { label: string; value: string | number }[],
  value?: string | number | null,
) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function getOptionType(
  options: readonly { type?: string; value: string | number }[],
  value?: string | number | null,
) {
  return options.find((item) => item.value === value)?.type || 'info'
}
