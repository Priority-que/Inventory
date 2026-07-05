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
  { label: '采购经理', value: 3, code: 'PURCHASE_MANAGER' },
  { label: '仓库岗', value: 4, code: 'WAREHOUSE' },
  { label: '供应商', value: 5, code: 'SUPPLIER' },
] as const

export const fileTypeOptions = [
  { label: '营业执照', value: 'BUSINESS_LICENSE' },
  { label: '资质文件', value: 'QUALIFICATION' },
  { label: '银行开户许可证', value: 'BANK_LICENSE' },
  { label: '其他', value: 'OTHER' },
] as const

export const intentOptions = [
  { label: '通用知识', value: 'COMMON' },
  { label: '业务待办', value: 'BUSINESS_TODO' },
  { label: '业务问答', value: 'BUSINESS_QA' },
  { label: '业务知识问答', value: 'BUSINESS_KNOWLEDGE_QA' },
] as const

export const purchaseRequestStatusOptions = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '待审批', value: 'PENDING_APPROVAL', type: 'warning' },
  { label: '已通过', value: 'APPROVED', type: 'success' },
  { label: '已驳回', value: 'REJECTED', type: 'danger' },
  { label: '已撤回', value: 'WITHDRAWN', type: 'info' },
  { label: '已生成订单', value: 'ORDER_CREATED', type: 'success' },
] as const

export const purchaseReviewActionOptions = [
  { label: '首次提交', value: 'SUBMIT', type: 'warning' },
  { label: '重新提交', value: 'RESUBMIT', type: 'warning' },
  { label: '撤回', value: 'WITHDRAW', type: 'info' },
  { label: '审批通过', value: 'APPROVE', type: 'success' },
  { label: '审批驳回', value: 'REJECT', type: 'danger' },
] as const

export const purchaseOrderStatusOptions = [
  { label: '待确认', value: 'WAIT_CONFIRM', type: 'warning' },
  { label: '执行中', value: 'IN_PROGRESS', type: 'primary' },
  { label: '部分到货', value: 'PARTIAL_ARRIVAL', type: 'warning' },
  { label: '待入库', value: 'WAIT_INBOUND', type: 'warning' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '已关闭', value: 'CLOSED', type: 'info' },
  { label: '已取消', value: 'CANCELLED', type: 'danger' },
] as const

export const arrivalStatusOptions = [
  { label: '正常到货', value: 'NORMAL', type: 'success' },
  { label: '异常到货', value: 'ABNORMAL', type: 'danger' },
] as const

export const inboundStatusOptions = [
  { label: '待确认入库', value: 'PENDING', type: 'warning' },
  { label: '已完成入库', value: 'COMPLETED', type: 'success' },
  { label: '已取消', value: 'CANCELLED', type: 'info' },
  { label: '异常', value: 'ABNORMAL', type: 'danger' },
] as const

export const riskLevelOptions = [
  { label: '高风险', value: 'HIGH', type: 'danger' },
  { label: '中风险', value: 'MEDIUM', type: 'warning' },
  { label: '低风险', value: 'LOW', type: 'info' },
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
