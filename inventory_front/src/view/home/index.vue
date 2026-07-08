<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, type LocationQueryRaw } from 'vue-router'
import { getArrivalPageApi } from '@/api/arrival'
import { getInboundPageApi } from '@/api/inbound'
import { getInventoryPageApi } from '@/api/inventory'
import { getPurchaseOrderPageApi, getSupplierPurchaseOrderPageApi } from '@/api/purchaseOrder'
import { getMyApprovedPurchaseRequestPageApi, getPurchaseRequestPageApi } from '@/api/purchaseRequest'
import { useAuthStore } from '@/stores/auth'

interface StatItem {
  label: string
  value: number
  desc: string
}

interface TodoItem {
  key: string
  title: string
  count: number
  desc: string
  priority: '高' | '中' | '低'
  priorityType: 'danger' | 'warning' | 'info' | 'primary'
  path: string
  query?: LocationQueryRaw
  actionText: string
}

const loading = ref(false)
const router = useRouter()
const authStore = useAuthStore()

const stats = ref<StatItem[]>([])
const todos = ref<TodoItem[]>([])

const roleCodes = computed(() => authStore.user?.roleCodes || [])
const hasRole = (role: string) => roleCodes.value.includes(role)

function primaryRole() {
  return roleCodes.value[0] || ''
}

function inventoryPath() {
  if (hasRole('WAREHOUSE')) {
    return '/warehouse/inventory'
  }
  if (hasRole('PURCHASER')) {
    return '/purchaser/inventory'
  }
  if (hasRole('PURCHASE_MANAGER')) {
    return '/manager/inventory'
  }
  return '/admin/inventory'
}

async function getTotal<T>(loader: () => Promise<{ total: number; records: T[] }>) {
  const result = await loader()
  return result.total || 0
}

async function getOrderCount(status: string) {
  return getTotal(() => getPurchaseOrderPageApi({ pageNum: 1, pageSize: 1, status }))
}

async function getSupplierOrderCount(status: string) {
  return getTotal(() => getSupplierPurchaseOrderPageApi({ pageNum: 1, pageSize: 1, status }))
}

async function getInventoryAlertCounts() {
  const [low, over] = await Promise.all([
    getTotal(() => getInventoryPageApi({ pageNum: 1, pageSize: 1, stockStatus: 'LOW' })),
    getTotal(() => getInventoryPageApi({ pageNum: 1, pageSize: 1, stockStatus: 'OVER' })),
  ])
  return {
    low,
    over,
    total: low + over,
  }
}

async function buildPurchaserWorkbench() {
  const [
    approvedRequestCount,
    waitConfirmOrderCount,
    inProgressOrderCount,
    partialArrivalOrderCount,
    pendingArrivalCount,
    pendingInboundCount,
    inventoryAlerts,
  ] = await Promise.all([
    getTotal(() => getMyApprovedPurchaseRequestPageApi({ pageNum: 1, pageSize: 1 })),
    getOrderCount('WAIT_CONFIRM'),
    getOrderCount('IN_PROGRESS'),
    getOrderCount('PARTIAL_ARRIVAL'),
    getTotal(() => getArrivalPageApi({ pageNum: 1, pageSize: 1, pendingInboundOnly: true })),
    getTotal(() => getInboundPageApi({ pageNum: 1, pageSize: 1, status: 'PENDING' })),
    getInventoryAlertCounts(),
  ])
  const activeOrderCount = inProgressOrderCount + partialArrivalOrderCount
  stats.value = [
    { label: '已审批未下单', value: approvedRequestCount, desc: '可继续生成采购订单' },
    { label: '待供应商确认', value: waitConfirmOrderCount, desc: '已下单，等待供应商确认交期' },
    { label: '执行中/部分到货', value: activeOrderCount, desc: '供应商已确认，正在履约' },
    { label: '待生成入库单', value: pendingArrivalCount, desc: '到货已登记，等待仓库生成入库单' },
    { label: '待确认入库单', value: pendingInboundCount, desc: '入库单已生成，等待仓库确认' },
    { label: '库存预警', value: inventoryAlerts.total, desc: `低库存 ${inventoryAlerts.low}，超储 ${inventoryAlerts.over}` },
  ]
  todos.value = [
    {
      key: 'approvedRequest',
      title: '已审批但未创建订单',
      count: approvedRequestCount,
      desc: '采购申请已通过审批，可进入采购订单生成环节。',
      priority: '高',
      priorityType: 'danger',
      path: '/purchaser/order',
      actionText: '生成订单',
    },
    {
      key: 'waitConfirmOrder',
      title: '待供应商确认订单',
      count: waitConfirmOrderCount,
      desc: '订单已创建，等待供应商确认承诺到货日期。',
      priority: '中',
      priorityType: 'warning',
      path: '/purchaser/order',
      query: { status: 'WAIT_CONFIRM' },
      actionText: '查看订单',
    },
    {
      key: 'activeOrder',
      title: '执行中 / 部分到货订单',
      count: activeOrderCount,
      desc: '订单正在履约，可跟踪到货和入库进度。',
      priority: '中',
      priorityType: 'warning',
      path: '/purchaser/order',
      query: { status: inProgressOrderCount > 0 ? 'IN_PROGRESS' : 'PARTIAL_ARRIVAL' },
      actionText: '跟踪订单',
    },
    {
      key: 'pendingInbound',
      title: '待确认入库单',
      count: pendingInboundCount,
      desc: '入库单已生成，等待仓库确认后更新库存台账。',
      priority: '低',
      priorityType: 'info',
      path: '/purchaser/inbound',
      query: { status: 'PENDING' },
      actionText: '查看入库',
    },
  ]
}

async function buildManagerWorkbench() {
  const [pendingApprovalCount, inventoryAlerts] = await Promise.all([
    getTotal(() => getPurchaseRequestPageApi({ pageNum: 1, pageSize: 1, status: 'PENDING_APPROVAL' })),
    getInventoryAlertCounts(),
  ])
  stats.value = [
    { label: '待审批采购申请', value: pendingApprovalCount, desc: '采购员提交后等待审批' },
    { label: '库存预警', value: inventoryAlerts.total, desc: `低库存 ${inventoryAlerts.low}，超储 ${inventoryAlerts.over}` },
  ]
  todos.value = [
    {
      key: 'pendingApproval',
      title: '待审批采购申请',
      count: pendingApprovalCount,
      desc: '采购员已提交申请，需要采购经理审批通过或驳回。',
      priority: '高',
      priorityType: 'danger',
      path: '/manager/approval',
      query: { status: 'PENDING_APPROVAL' },
      actionText: '去审批',
    },
    {
      key: 'inventoryAlert',
      title: '库存预警',
      count: inventoryAlerts.total,
      desc: `低库存 ${inventoryAlerts.low} 条，超储 ${inventoryAlerts.over} 条，需要关注补货或库存占用。`,
      priority: inventoryAlerts.low > 0 ? '中' : '低',
      priorityType: inventoryAlerts.low > 0 ? 'warning' : 'info',
      path: inventoryPath(),
      query: { stockStatus: inventoryAlerts.low > 0 ? 'LOW' : 'OVER' },
      actionText: '查看库存',
    },
  ]
}

async function buildWarehouseWorkbench() {
  const [
    inProgressOrderCount,
    partialArrivalOrderCount,
    pendingArrivalCount,
    pendingInboundCount,
    abnormalArrivalCount,
    inventoryAlerts,
  ] = await Promise.all([
    getOrderCount('IN_PROGRESS'),
    getOrderCount('PARTIAL_ARRIVAL'),
    getTotal(() => getArrivalPageApi({ pageNum: 1, pageSize: 1, pendingInboundOnly: true })),
    getTotal(() => getInboundPageApi({ pageNum: 1, pageSize: 1, status: 'PENDING' })),
    getTotal(() => getArrivalPageApi({ pageNum: 1, pageSize: 1, status: 'ABNORMAL' })),
    getInventoryAlertCounts(),
  ])
  const activeOrderCount = inProgressOrderCount + partialArrivalOrderCount
  stats.value = [
    { label: '可登记到货订单', value: activeOrderCount, desc: '执行中或部分到货订单' },
    { label: '待生成入库单', value: pendingArrivalCount, desc: '已有到货记录，尚未生成有效入库单' },
    { label: '待确认入库单', value: pendingInboundCount, desc: '入库单已生成，待仓库确认' },
    { label: '异常到货', value: abnormalArrivalCount, desc: '存在不合格数量或异常说明' },
    { label: '库存预警', value: inventoryAlerts.total, desc: `低库存 ${inventoryAlerts.low}，超储 ${inventoryAlerts.over}` },
  ]
  todos.value = [
    {
      key: 'activeOrder',
      title: '执行中 / 部分到货订单',
      count: activeOrderCount,
      desc: '供应商已确认订单，可根据实际到货情况登记到货。',
      priority: '高',
      priorityType: 'danger',
      path: '/warehouse/arrival',
      actionText: '登记到货',
    },
    {
      key: 'pendingArrival',
      title: '待生成入库单的到货单',
      count: pendingArrivalCount,
      desc: '到货记录已有合格数量，需要生成入库单。',
      priority: '高',
      priorityType: 'danger',
      path: '/warehouse/inbound',
      query: { todo: 'pendingArrival' },
      actionText: '生成入库单',
    },
    {
      key: 'pendingInbound',
      title: '待确认入库单',
      count: pendingInboundCount,
      desc: '入库单已生成，确认后会更新库存台账和库存流水。',
      priority: '高',
      priorityType: 'danger',
      path: '/warehouse/inbound',
      query: { status: 'PENDING' },
      actionText: '确认入库',
    },
    {
      key: 'abnormalArrival',
      title: '异常到货',
      count: abnormalArrivalCount,
      desc: '存在不合格数量或异常说明的到货记录。',
      priority: '中',
      priorityType: 'warning',
      path: '/warehouse/arrival',
      query: { status: 'ABNORMAL' },
      actionText: '查看异常',
    },
    {
      key: 'inventoryAlert',
      title: '库存预警',
      count: inventoryAlerts.total,
      desc: `低库存 ${inventoryAlerts.low} 条，超储 ${inventoryAlerts.over} 条。`,
      priority: inventoryAlerts.low > 0 ? '中' : '低',
      priorityType: inventoryAlerts.low > 0 ? 'warning' : 'info',
      path: inventoryPath(),
      query: { stockStatus: inventoryAlerts.low > 0 ? 'LOW' : 'OVER' },
      actionText: '查看库存',
    },
  ]
}

async function buildSupplierWorkbench() {
  const [waitConfirmOrderCount, inProgressOrderCount, partialArrivalOrderCount] = await Promise.all([
    getSupplierOrderCount('WAIT_CONFIRM'),
    getSupplierOrderCount('IN_PROGRESS'),
    getSupplierOrderCount('PARTIAL_ARRIVAL'),
  ])
  const activeOrderCount = inProgressOrderCount + partialArrivalOrderCount
  stats.value = [
    { label: '待确认订单', value: waitConfirmOrderCount, desc: '等待确认承诺到货日期' },
    { label: '执行中/部分到货', value: activeOrderCount, desc: '已确认，正在履约' },
  ]
  todos.value = [
    {
      key: 'waitConfirmOrder',
      title: '待供应商确认订单',
      count: waitConfirmOrderCount,
      desc: '采购方已下单，需要确认承诺到货日期。',
      priority: '高',
      priorityType: 'danger',
      path: '/supplier/order',
      query: { status: 'WAIT_CONFIRM' },
      actionText: '确认交期',
    },
    {
      key: 'activeOrder',
      title: '执行中 / 部分到货订单',
      count: activeOrderCount,
      desc: '已确认订单正在履约，可查看到货和入库进度。',
      priority: '中',
      priorityType: 'warning',
      path: '/supplier/order',
      query: { status: inProgressOrderCount > 0 ? 'IN_PROGRESS' : 'PARTIAL_ARRIVAL' },
      actionText: '查看履约',
    },
  ]
}

async function buildAdminWorkbench() {
  const [pendingApprovalCount, waitConfirmOrderCount, pendingInboundCount, abnormalArrivalCount, inventoryAlerts] =
    await Promise.all([
      getTotal(() => getPurchaseRequestPageApi({ pageNum: 1, pageSize: 1, status: 'PENDING_APPROVAL' })),
      getOrderCount('WAIT_CONFIRM'),
      getTotal(() => getInboundPageApi({ pageNum: 1, pageSize: 1, status: 'PENDING' })),
      getTotal(() => getArrivalPageApi({ pageNum: 1, pageSize: 1, status: 'ABNORMAL' })),
      getInventoryAlertCounts(),
    ])
  stats.value = [
    { label: '待审批采购申请', value: pendingApprovalCount, desc: '业务待办总览' },
    { label: '待供应商确认', value: waitConfirmOrderCount, desc: '订单协同状态' },
    { label: '待确认入库单', value: pendingInboundCount, desc: '仓储待办状态' },
    { label: '异常到货', value: abnormalArrivalCount, desc: '到货异常记录' },
    { label: '库存预警', value: inventoryAlerts.total, desc: `低库存 ${inventoryAlerts.low}，超储 ${inventoryAlerts.over}` },
  ]
  todos.value = [
    {
      key: 'inventoryAlert',
      title: '库存预警',
      count: inventoryAlerts.total,
      desc: `低库存 ${inventoryAlerts.low} 条，超储 ${inventoryAlerts.over} 条，可在库存台账查看。`,
      priority: inventoryAlerts.low > 0 ? '中' : '低',
      priorityType: inventoryAlerts.low > 0 ? 'warning' : 'info',
      path: '/admin/inventory',
      query: { stockStatus: inventoryAlerts.low > 0 ? 'LOW' : 'OVER' },
      actionText: '查看库存',
    },
  ]
}

const activeTodos = computed(() => todos.value.filter((item) => item.count > 0))

async function loadWorkbench() {
  loading.value = true
  try {
    const role = primaryRole()
    if (role === 'PURCHASER') {
      await buildPurchaserWorkbench()
    } else if (role === 'PURCHASE_MANAGER') {
      await buildManagerWorkbench()
    } else if (role === 'WAREHOUSE') {
      await buildWarehouseWorkbench()
    } else if (role === 'SUPPLIER') {
      await buildSupplierWorkbench()
    } else {
      await buildAdminWorkbench()
    }
  } finally {
    loading.value = false
  }
}

function goTodo(item: TodoItem) {
  router.push({
    path: item.path,
    query: item.query,
  })
}

onMounted(loadWorkbench)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-subtitle">查看采购、库存和供应商的关键业务状态</p>
      </div>
    </div>

    <div v-loading="loading" class="stats-grid">
      <el-card v-for="item in stats" :key="item.label" shadow="never">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-desc">{{ item.desc }}</div>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="todo-header">
          <span>业务待办</span>
          <el-button link type="primary" :loading="loading" @click="loadWorkbench">刷新</el-button>
        </div>
      </template>
      <div v-loading="loading" class="todo-list">
        <div v-for="item in activeTodos" :key="item.key" class="todo-item">
          <div class="todo-main">
            <div class="todo-title-row">
              <span class="todo-title">{{ item.title }}</span>
              <el-tag :type="item.priorityType" effect="plain">{{ item.priority }}优先级</el-tag>
            </div>
            <div class="todo-desc">{{ item.desc }}</div>
          </div>
          <div class="todo-count">
            <strong>{{ item.count }}</strong>
            <span>项</span>
          </div>
          <el-button type="primary" plain @click="goTodo(item)">
            {{ item.actionText }}
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-empty v-if="!activeTodos.length" description="当前角色暂无待处理业务" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 266px;
}

.stat-value {
  color: #111827;
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}

.stat-desc {
  margin-top: 6px;
  color: #8a97aa;
  font-size: 12px;
}

.todo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.todo-list {
  min-height: 168px;
}

.todo-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 18px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.todo-item:last-child {
  border-bottom: 0;
}

.todo-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-title {
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.todo-desc {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.todo-count {
  min-width: 72px;
  text-align: right;
}

.todo-count strong {
  color: #111827;
  font-size: 24px;
}

.todo-count span {
  margin-left: 4px;
  color: #6b7280;
  font-size: 13px;
}

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .todo-item {
    grid-template-columns: minmax(0, 1fr);
    align-items: stretch;
  }

  .todo-count {
    text-align: left;
  }
}
</style>
