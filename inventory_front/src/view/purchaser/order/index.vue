<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addPurchaseOrderApi,
  cancelPurchaseOrderApi,
  confirmPurchaseOrderApi,
  closePurchaseOrderApi,
  getPurchaseOrderByIdApi,
  getPurchaseOrderItemByIdApi,
  getPurchaseOrderItemsByOrderIdApi,
  getPurchaseOrderPageApi,
  getSupplierPurchaseOrderPageApi,
  updatePurchaseOrderApi,
  updatePurchaseOrderItemApi,
  type PurchaseOrderDTO,
  type PurchaseOrderItemCreateDTO,
  type PurchaseOrderItemDTO,
  type PurchaseOrderItemVO,
  type PurchaseOrderVO,
} from '@/api/purchaseOrder'
import {
  getMyApprovedPurchaseRequestPageApi,
  getPurchaseRequestItemsByRequestIdApi,
  type PurchaseRequestItemVO,
  type PurchaseRequestPageVO,
} from '@/api/purchaseRequest'
import { getSupplierPageApi, type SupplierVO } from '@/api/supplier'
import { getOptionLabel, getOptionType, purchaseOrderStatusOptions } from '@/constants/business'
import { formatDate, formatDateTime, formatEmpty } from '@/utils/format'

interface DraftOrderItem extends PurchaseOrderItemCreateDTO {
  id?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  requestNumber?: number
  orderNumber?: number
  sortNumber?: number
  lineAmount?: number
  arrivedNumber?: number
  inboundNumber?: number
}

const route = useRoute()

const isPurchaserRoute = computed(() => route.path.startsWith('/purchaser'))
const isSupplierRoute = computed(() => route.path.startsWith('/supplier'))
const canManage = computed(() => isPurchaserRoute.value)
const pageTitle = computed(() => String(route.meta.title || '采购订单'))
const pageSubtitle = computed(() =>
  isPurchaserRoute.value
    ? '基于已审批采购申请生成订单，维护供应商、交期、单价和执行状态'
    : isSupplierRoute.value
      ? '查看分配给本供应商的采购订单，确认承诺到货日期并跟踪履约状态'
      : '查看采购订单、明细履约与上下游流转字段',
)

const supplierStatusTabs = [
  { label: '待确认', value: 'WAIT_CONFIRM' },
  { label: '已确认/履约中', value: 'IN_PROGRESS' },
  { label: '部分到货', value: 'PARTIAL_ARRIVAL' },
  { label: '待入库', value: 'WAIT_INBOUND' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '全部', value: '' },
]

const loading = ref(false)
const tableData = ref<PurchaseOrderVO[]>([])
const total = ref(0)

function defaultOrderStatus() {
  return isSupplierRoute.value ? 'WAIT_CONFIRM' : ''
}

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  requestTitle: '',
  supplierName: '',
  purchaseName: '',
  planDateBegin: '',
  planDateEnd: '',
  status: defaultOrderStatus(),
})
const planDateRange = ref<string[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const requestItemsLoading = ref(false)
const formRef = ref<FormInstance>()
const orderForm = reactive<PurchaseOrderDTO>({
  requestId: undefined,
  supplierId: undefined,
  purchaserId: undefined,
  planDate: '',
  supplierDate: '',
  status: 'WAIT_CONFIRM',
  supplierNote: '',
  closeReason: '',
  remark: '',
  items: [],
})
const requestItemRows = ref<DraftOrderItem[]>([])
const selectedRequest = ref<PurchaseRequestPageVO | null>(null)
const selectedSupplier = ref<SupplierVO | null>(null)

const requestDialogVisible = ref(false)
const requestLoading = ref(false)
const requestRows = ref<PurchaseRequestPageVO[]>([])
const requestTotal = ref(0)
const requestQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  requestNo: '',
  title: '',
  dept: '',
  status: 'APPROVED',
})

const supplierDialogVisible = ref(false)
const supplierLoading = ref(false)
const supplierRows = ref<SupplierVO[]>([])
const supplierTotal = ref(0)
const supplierQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  code: '',
  name: '',
  contactName: '',
  contactPhone: '',
  status: 'ACTIVE',
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<PurchaseOrderVO | null>(null)
const detailItems = ref<PurchaseOrderItemVO[]>([])
const detailTab = ref('base')

const itemDialogVisible = ref(false)
const itemFormLoading = ref(false)
const itemFormRef = ref<FormInstance>()
const itemForm = reactive<PurchaseOrderItemDTO>({
  id: undefined,
  unitPrice: undefined,
  remark: '',
})
const editingItem = ref<PurchaseOrderItemVO | null>(null)

const confirmDialogVisible = ref(false)
const confirmLoading = ref(false)
const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive<PurchaseOrderDTO>({
  id: undefined,
  orderNo: '',
  planDate: '',
  supplierDate: '',
  supplierNote: '',
})
const confirmOrder = ref<PurchaseOrderVO | null>(null)
const confirmItems = ref<PurchaseOrderItemVO[]>([])
const confirmItemsLoading = ref(false)

const formRules: FormRules<PurchaseOrderDTO> = {
  requestId: [{ required: true, message: '请选择已通过的采购申请', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划到货日期', trigger: 'change' }],
}

const confirmRules: FormRules<PurchaseOrderDTO> = {
  supplierDate: [{ required: true, message: '请选择供应商承诺日期', trigger: 'change' }],
}

const itemFormRules: FormRules<PurchaseOrderItemDTO> = {
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
}

function syncPlanDateRange() {
  query.planDateBegin = planDateRange.value[0] || ''
  query.planDateEnd = planDateRange.value[1] || ''
}

async function loadData() {
  syncPlanDateRange()
  loading.value = true
  try {
    const result = isSupplierRoute.value
      ? await getSupplierPurchaseOrderPageApi(query)
      : await getPurchaseOrderPageApi(query)
    tableData.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    requestTitle: '',
    supplierName: '',
    purchaseName: '',
    planDateBegin: '',
    planDateEnd: '',
    status: defaultOrderStatus(),
  })
  planDateRange.value = []
  loadData()
}

function handleSupplierStatusChange() {
  query.pageNum = 1
  loadData()
}

function resetForm() {
  Object.assign(orderForm, {
    id: undefined,
    orderNo: '',
    requestId: undefined,
    supplierId: undefined,
    purchaserId: undefined,
    planDate: '',
    supplierDate: '',
    confirmTime: '',
    totalAmount: undefined,
    status: 'WAIT_CONFIRM',
    supplierNote: '',
    closeTime: '',
    closeReason: '',
    remark: '',
    createTime: '',
    updateTime: '',
    deleted: undefined,
    items: [],
  })
  requestItemRows.value = []
  selectedRequest.value = null
  selectedSupplier.value = null
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(row: PurchaseOrderVO) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(orderForm, {
    id: row.id,
    orderNo: row.orderNo,
    requestId: row.requestId,
    supplierId: row.supplierId,
    purchaserId: row.purchaserId,
    planDate: row.planDate,
    supplierDate: row.supplierDate,
    confirmTime: row.confirmTime,
    totalAmount: row.totalAmount,
    status: row.status,
    supplierNote: row.supplierNote,
    closeTime: row.closeTime,
    closeReason: row.closeReason,
    remark: row.remark,
    createTime: row.createTime,
    updateTime: row.updateTime,
    deleted: row.deleted,
  })
  selectedRequest.value = {
    id: row.requestId || 0,
    requestNo: '',
    title: row.requestTitle,
    status: '',
  }
  selectedSupplier.value = {
    id: row.supplierId || 0,
    name: row.supplierName,
  }
  dialogVisible.value = true
  await loadOrderItemsForEdit(row.id)
}

function mapRequestItems(items: PurchaseRequestItemVO[]) {
  requestItemRows.value = items.map((item) => ({
    requestItemId: item.id,
    materialId: item.materialId,
    materialCode: item.materialCode,
    materialName: item.materialName,
    specification: item.specification,
    unit: item.unit,
    requestNumber: item.requestNumber,
    sortNumber: item.sortNumber,
    unitPrice: undefined,
    remark: item.remark || '',
    status: '',
  }))
}

async function loadRequestItems() {
  if (!orderForm.requestId) {
    ElMessage.warning('请先选择采购申请')
    return
  }

  requestItemsLoading.value = true
  try {
    const items = await getPurchaseRequestItemsByRequestIdApi(orderForm.requestId)
    mapRequestItems(items)
    ElMessage.success(`已加载 ${items.length} 条申请明细`)
  } finally {
    requestItemsLoading.value = false
  }
}

async function loadOrderItemsForEdit(orderId: number) {
  requestItemsLoading.value = true
  try {
    const items = await getPurchaseOrderItemsByOrderIdApi(orderId)
    requestItemRows.value = items.map((item) => ({
      id: item.id,
      requestItemId: item.requestItemId,
      materialId: item.materialId,
      materialCode: item.materialCode,
      materialName: item.materialName,
      specification: item.specification,
      unit: item.unit,
      orderNumber: item.orderNumber,
      sortNumber: item.sortNumber,
      unitPrice: item.unitPrice,
      remark: item.remark,
      lineAmount: item.lineAmount,
      arrivedNumber: item.arrivedNumber,
      inboundNumber: item.inboundNumber,
      status: '',
    }))
  } finally {
    requestItemsLoading.value = false
  }
}

async function submitForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      if (!requestItemRows.value.length) {
        ElMessage.warning('请先加载并维护订单明细')
        return
      }
      await addPurchaseOrderApi({
        ...orderForm,
        items: requestItemRows.value.map((item) => ({
          requestItemId: item.requestItemId,
          unitPrice: item.unitPrice,
          remark: item.remark,
        })),
      })
      ElMessage.success('采购订单已新增')
    } else {
      await updatePurchaseOrderApi({
        ...orderForm,
        items: undefined,
      })
      ElMessage.success('采购订单已更新')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    formLoading.value = false
  }
}

function canEditRow(row: PurchaseOrderVO) {
  return canManage.value && row.status === 'WAIT_CONFIRM'
}

async function loadSelectableRequests() {
  requestLoading.value = true
  try {
    const result = await getMyApprovedPurchaseRequestPageApi(requestQuery)
    requestRows.value = result.records
    requestTotal.value = result.total
  } finally {
    requestLoading.value = false
  }
}

function openRequestDialog() {
  requestDialogVisible.value = true
  if (!requestRows.value.length) {
    loadSelectableRequests()
  }
}

function handleRequestSearch() {
  requestQuery.pageNum = 1
  loadSelectableRequests()
}

function handleRequestReset() {
  Object.assign(requestQuery, {
    pageNum: 1,
    pageSize: 10,
    requestNo: '',
    title: '',
    dept: '',
    status: 'APPROVED',
  })
  loadSelectableRequests()
}

async function selectRequest(row: PurchaseRequestPageVO) {
  selectedRequest.value = row
  orderForm.requestId = row.id
  orderForm.purchaserId = row.applicantId
  if (!orderForm.planDate && row.expectedDate) {
    orderForm.planDate = row.expectedDate
  }
  requestDialogVisible.value = false
  formRef.value?.validateField('requestId')
  await loadRequestItems()
}

function handleRequestSizeChange(size: number) {
  requestQuery.pageSize = size
  requestQuery.pageNum = 1
  loadSelectableRequests()
}

function handleRequestCurrentChange(page: number) {
  requestQuery.pageNum = page
  loadSelectableRequests()
}

async function loadSelectableSuppliers() {
  supplierLoading.value = true
  try {
    const result = await getSupplierPageApi(supplierQuery)
    supplierRows.value = result.records
    supplierTotal.value = result.total
  } finally {
    supplierLoading.value = false
  }
}

function openSupplierDialog() {
  supplierDialogVisible.value = true
  if (!supplierRows.value.length) {
    loadSelectableSuppliers()
  }
}

function handleSupplierSearch() {
  supplierQuery.pageNum = 1
  loadSelectableSuppliers()
}

function handleSupplierReset() {
  Object.assign(supplierQuery, {
    pageNum: 1,
    pageSize: 10,
    code: '',
    name: '',
    contactName: '',
    contactPhone: '',
    status: 'ACTIVE',
  })
  loadSelectableSuppliers()
}

function selectSupplier(row: SupplierVO) {
  selectedSupplier.value = row
  orderForm.supplierId = row.id
  supplierDialogVisible.value = false
  formRef.value?.validateField('supplierId')
}

function handleSupplierSizeChange(size: number) {
  supplierQuery.pageSize = size
  supplierQuery.pageNum = 1
  loadSelectableSuppliers()
}

function handleSupplierCurrentChange(page: number) {
  supplierQuery.pageNum = page
  loadSelectableSuppliers()
}

function canCancelRow(row: PurchaseOrderVO) {
  return canManage.value && row.status === 'WAIT_CONFIRM'
}

function canCloseRow(row: PurchaseOrderVO) {
  return canManage.value && ['IN_PROGRESS', 'PARTIAL_ARRIVAL'].includes(row.status || '')
}

function canConfirmRow(row: PurchaseOrderVO) {
  return isSupplierRoute.value && row.status === 'WAIT_CONFIRM'
}

async function handleCancel(row: PurchaseOrderVO) {
  await ElMessageBox.confirm(`确定取消采购订单 ${row.orderNo || row.id} 吗？`, '取消采购订单', {
    type: 'warning',
    confirmButtonText: '取消订单',
    cancelButtonText: '返回',
  })

  await cancelPurchaseOrderApi({ id: row.id })
  ElMessage.success('采购订单已取消')
  loadData()
  if (detail.value?.id === row.id) {
    loadDetail(row.id)
  }
}

async function handleClose(row: PurchaseOrderVO) {
  const { value } = await ElMessageBox.prompt('请输入关闭原因', '关闭采购订单', {
    confirmButtonText: '关闭订单',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：供应商停供，订单结束',
  })

  await closePurchaseOrderApi({
    id: row.id,
    closeReason: value,
  })
  ElMessage.success('采购订单已关闭')
  loadData()
  if (detail.value?.id === row.id) {
    loadDetail(row.id)
  }
}

async function openConfirmDialog(row: PurchaseOrderVO) {
  Object.assign(confirmForm, {
    id: row.id,
    orderNo: row.orderNo,
    planDate: row.planDate,
    supplierDate: row.supplierDate || row.planDate || '',
    supplierNote: row.supplierNote || '',
  })
  confirmOrder.value = row
  confirmItems.value = []
  confirmFormRef.value?.clearValidate()
  confirmDialogVisible.value = true
  confirmItemsLoading.value = true
  try {
    confirmItems.value = await getPurchaseOrderItemsByOrderIdApi(row.id)
  } finally {
    confirmItemsLoading.value = false
  }
}

async function submitConfirmForm() {
  await confirmFormRef.value?.validate()
  confirmLoading.value = true
  try {
    const confirmedId = confirmForm.id
    await confirmPurchaseOrderApi({
      id: confirmedId,
      supplierDate: confirmForm.supplierDate,
      supplierNote: confirmForm.supplierNote,
    })
    ElMessage.success('采购订单已确认')
    confirmDialogVisible.value = false
    confirmOrder.value = null
    confirmItems.value = []
    await loadData()
    if (confirmedId && detail.value?.id === confirmedId) {
      await loadDetail(confirmedId)
    }
  } finally {
    confirmLoading.value = false
  }
}

function closeConfirmDialog() {
  confirmDialogVisible.value = false
  confirmOrder.value = null
  confirmItems.value = []
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    const [base, items] = await Promise.all([getPurchaseOrderByIdApi(id), getPurchaseOrderItemsByOrderIdApi(id)])
    detail.value = base
    detailItems.value = items
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: PurchaseOrderVO) {
  detailTab.value = 'base'
  detailVisible.value = true
  await loadDetail(row.id)
}

function openEditItemDialog(row: PurchaseOrderItemVO) {
  editingItem.value = row
  Object.assign(itemForm, {
    id: row.id,
    unitPrice: row.unitPrice,
    remark: row.remark,
  })
  itemFormRef.value?.clearValidate()
  itemDialogVisible.value = true
}

async function submitItemForm() {
  await itemFormRef.value?.validate()
  itemFormLoading.value = true
  try {
    await updatePurchaseOrderItemApi({ ...itemForm })
    ElMessage.success('订单明细已更新')
    itemDialogVisible.value = false
    if (detail.value?.id) {
      await loadDetail(detail.value.id)
    }
  } finally {
    itemFormLoading.value = false
  }
}

async function refreshEditingItem() {
  if (!itemForm.id) {
    return
  }

  editingItem.value = await getPurchaseOrderItemByIdApi(itemForm.id)
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

function handleCurrentChange(page: number) {
  query.pageNum = page
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ pageTitle }}</h2>
        <p class="page-subtitle">{{ pageSubtitle }}</p>
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        从申请生成订单
      </el-button>
    </div>

    <section class="content-panel">
      <div v-if="isSupplierRoute" class="supplier-status-tabs">
        <el-radio-group v-model="query.status" @change="handleSupplierStatusChange">
          <el-radio-button v-for="item in supplierStatusTabs" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="订单号">
            <el-input v-model="query.orderNo" clearable placeholder="输入采购订单号" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="申请标题">
            <el-input v-model="query.requestTitle" clearable placeholder="输入申请标题" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item v-if="!isSupplierRoute" label="供应商名称">
            <el-input v-model="query.supplierName" clearable placeholder="输入供应商名称" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="采购员">
            <el-input v-model="query.purchaseName" clearable placeholder="输入采购员姓名" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="计划到货日期">
            <el-date-picker
              v-model="planDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>
          <el-form-item v-if="!isSupplierRoute" label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in purchaseOrderStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed">
          <el-table-column prop="orderNo" label="订单号" min-width="160" fixed="left" show-overflow-tooltip />
          <el-table-column prop="requestTitle" label="申请标题" min-width="180" show-overflow-tooltip />
          <el-table-column v-if="!isSupplierRoute" prop="supplierName" label="供应商名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="purchaserName" label="采购员" min-width="120" show-overflow-tooltip />
          <el-table-column label="计划到货日期" min-width="128">
            <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
          </el-table-column>
          <el-table-column label="供应商承诺日期" min-width="138">
            <template #default="{ row }">{{ formatDate(row.supplierDate) }}</template>
          </el-table-column>
          <el-table-column label="确认时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.confirmTime) }}</template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="总金额" min-width="120" />
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(purchaseOrderStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(purchaseOrderStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="isSupplierRoute ? '反馈备注' : '供应商备注'" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.supplierNote) }}</template>
          </el-table-column>
          <el-table-column v-if="!isSupplierRoute" label="关闭时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.closeTime) }}</template>
          </el-table-column>
          <el-table-column v-if="!isSupplierRoute" label="关闭原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.closeReason) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column v-if="!isSupplierRoute" label="创建时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column v-if="!isSupplierRoute" label="更新时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" :width="isSupplierRoute ? 150 : 250" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">
                  {{ isSupplierRoute ? '查看明细' : '详情' }}
                </el-button>
                <el-button v-if="canConfirmRow(row)" link type="success" @click="openConfirmDialog(row)">确认交期</el-button>
                <el-button v-if="canEditRow(row)" link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button v-if="canCancelRow(row)" link type="warning" @click="handleCancel(row)">取消</el-button>
                <el-button v-if="canCloseRow(row)" link type="danger" @click="handleClose(row)">关闭</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '从申请生成订单' : '编辑采购订单'"
      width="1160px"
      @closed="resetForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="orderForm" :rules="formRules" label-width="120px">
        <div class="dialog-grid">
          <el-form-item label="采购申请" prop="requestId">
            <el-input
              :model-value="
                selectedRequest
                  ? `${selectedRequest.requestNo || '未生成单号'} / ${selectedRequest.title || ''}`
                  : ''
              "
              readonly
              placeholder="选择已审批通过的采购申请"
            >
              <template #append>
                <el-button :disabled="dialogMode === 'edit'" @click="openRequestDialog">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="供应商" prop="supplierId">
            <el-input
              :model-value="selectedSupplier ? `${selectedSupplier.code || ''} / ${selectedSupplier.name || ''}` : ''"
              readonly
              placeholder="选择供应商"
            >
              <template #append>
                <el-button :disabled="dialogMode === 'edit'" @click="openSupplierDialog">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="orderForm.status" disabled>
              <el-option
                v-for="item in purchaseOrderStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="计划到货日期" prop="planDate">
            <el-date-picker
              v-model="orderForm.planDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
        </div>
        <el-descriptions v-if="selectedRequest || selectedSupplier" class="detail-descriptions" :column="2" border>
          <el-descriptions-item label="申请人">{{ formatEmpty(selectedRequest?.applicantName) }}</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ formatEmpty(selectedRequest?.dept) }}</el-descriptions-item>
          <el-descriptions-item label="期望到货日期">{{ formatDate(selectedRequest?.expectedDate) }}</el-descriptions-item>
          <el-descriptions-item label="供应商联系人">{{ formatEmpty(selectedSupplier?.contactName) }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" :rows="2" type="textarea" placeholder="填写备注" />
        </el-form-item>

        <section class="content-panel form-embed-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">订单明细字段</div>
              <div class="page-desc">
                明细来自采购申请，创建订单时维护单价和备注
              </div>
            </div>
            <el-button v-if="dialogMode === 'create'" :loading="requestItemsLoading" @click="loadRequestItems">
              <el-icon><Refresh /></el-icon>
              重新加载明细
            </el-button>
          </div>
          <div class="table-wrap">
            <el-table v-loading="requestItemsLoading" :data="requestItemRows" table-layout="fixed">
              <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="88" />
              <el-table-column label="申请数量 / 订单数量" min-width="140">
                <template #default="{ row }">
                  {{ formatEmpty(row.requestNumber ?? row.orderNumber) }}
                </template>
              </el-table-column>
              <el-table-column label="单价" min-width="132">
                <template #default="{ row }">
                  <el-input-number
                    v-if="dialogMode === 'create'"
                    v-model="row.unitPrice"
                    :min="0.01"
                    :precision="2"
                    controls-position="right"
                  />
                  <span v-else>{{ formatEmpty(row.unitPrice) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="180">
                <template #default="{ row }">
                  <el-input v-if="dialogMode === 'create'" v-model="row.remark" placeholder="填写备注" />
                  <span v-else>{{ formatEmpty(row.remark) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="已到货" width="100">
                <template #default="{ row }">{{ formatEmpty(row.arrivedNumber) }}</template>
              </el-table-column>
              <el-table-column label="已入库" width="100">
                <template #default="{ row }">{{ formatEmpty(row.inboundNumber) }}</template>
              </el-table-column>
              <el-table-column label="行金额" width="100">
                <template #default="{ row }">{{ formatEmpty(row.lineAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </section>

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="requestDialogVisible" title="选择已通过采购申请" width="980px">
      <div class="filter-section">
        <el-form :model="requestQuery" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="申请单号">
            <el-input v-model="requestQuery.requestNo" clearable placeholder="输入申请单号" @keyup.enter="handleRequestSearch" />
          </el-form-item>
          <el-form-item label="申请标题">
            <el-input v-model="requestQuery.title" clearable placeholder="输入申请标题" @keyup.enter="handleRequestSearch" />
          </el-form-item>
          <el-form-item label="申请部门">
            <el-input v-model="requestQuery.dept" clearable placeholder="输入申请部门" @keyup.enter="handleRequestSearch" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleRequestReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleRequestSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="table-wrap">
        <el-table v-loading="requestLoading" :data="requestRows" row-key="id" table-layout="fixed">
          <el-table-column prop="requestNo" label="申请单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="title" label="申请标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="applicantName" label="申请人" min-width="120" show-overflow-tooltip />
          <el-table-column prop="dept" label="申请部门" min-width="120" show-overflow-tooltip />
          <el-table-column label="期望到货日期" min-width="128">
            <template #default="{ row }">{{ formatDate(row.expectedDate) }}</template>
          </el-table-column>
          <el-table-column label="审批时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.reviewTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectRequest(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="requestQuery.pageNum"
          v-model:page-size="requestQuery.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="requestTotal"
          @size-change="handleRequestSizeChange"
          @current-change="handleRequestCurrentChange"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="supplierDialogVisible" title="选择供应商" width="980px">
      <div class="filter-section">
        <el-form :model="supplierQuery" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="供应商编码">
            <el-input v-model="supplierQuery.code" clearable placeholder="输入供应商编码" @keyup.enter="handleSupplierSearch" />
          </el-form-item>
          <el-form-item label="供应商名称">
            <el-input v-model="supplierQuery.name" clearable placeholder="输入供应商名称" @keyup.enter="handleSupplierSearch" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="supplierQuery.contactName" clearable placeholder="输入联系人" @keyup.enter="handleSupplierSearch" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleSupplierReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleSupplierSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="table-wrap">
        <el-table v-loading="supplierLoading" :data="supplierRows" row-key="id" table-layout="fixed">
          <el-table-column prop="code" label="供应商编码" min-width="140" show-overflow-tooltip />
          <el-table-column prop="name" label="供应商名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="contactName" label="联系人" min-width="120" show-overflow-tooltip />
          <el-table-column prop="contactPhone" label="联系电话" min-width="140" show-overflow-tooltip />
          <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectSupplier(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="supplierQuery.pageNum"
          v-model:page-size="supplierQuery.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="supplierTotal"
          @size-change="handleSupplierSizeChange"
          @current-change="handleSupplierCurrentChange"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="confirmDialogVisible" title="确认采购订单" width="760px" @closed="closeConfirmDialog">
      <el-descriptions v-if="confirmOrder" class="detail-descriptions confirm-summary" :column="2" border>
        <el-descriptions-item label="订单号">{{ formatEmpty(confirmOrder.orderNo) }}</el-descriptions-item>
        <el-descriptions-item label="申请标题">{{ formatEmpty(confirmOrder.requestTitle) }}</el-descriptions-item>
        <el-descriptions-item label="采购员">{{ formatEmpty(confirmOrder.purchaserName) }}</el-descriptions-item>
        <el-descriptions-item label="计划到货日期">{{ formatDate(confirmOrder.planDate) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ formatEmpty(confirmOrder.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="status-tag" :type="getOptionType(purchaseOrderStatusOptions, confirmOrder.status)" effect="plain">
            {{ getOptionLabel(purchaseOrderStatusOptions, confirmOrder.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div class="confirm-items">
        <div class="confirm-items__title">物料明细</div>
        <el-table v-loading="confirmItemsLoading" :data="confirmItems" max-height="220" table-layout="fixed">
          <el-table-column prop="materialCode" label="物料编码" min-width="128" show-overflow-tooltip />
          <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="76" />
          <el-table-column prop="orderNumber" label="订单数量" min-width="104" />
          <el-table-column prop="unitPrice" label="单价" min-width="96" />
        </el-table>
      </div>

      <el-form
        ref="confirmFormRef"
        v-loading="confirmLoading"
        :model="confirmForm"
        :rules="confirmRules"
        label-width="120px"
      >
        <el-form-item label="订单号">
          <el-input :model-value="confirmForm.orderNo || String(confirmForm.id || '')" disabled />
        </el-form-item>
        <el-form-item label="计划到货日期">
          <el-input :model-value="formatDate(confirmForm.planDate)" disabled />
        </el-form-item>
        <el-form-item label="承诺到货日期" prop="supplierDate">
          <el-date-picker
            v-model="confirmForm.supplierDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
        <el-form-item label="供应商备注">
          <el-input v-model="confirmForm.supplierNote" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeConfirmDialog">取消</el-button>
          <el-button type="primary" :loading="confirmLoading" @click="submitConfirmForm">确认交期</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="isSupplierRoute ? '采购订单详情' : pageTitle + '详情'" :size="isSupplierRoute ? '960px' : '1180px'">
      <div v-loading="detailLoading" class="detail-section">
        <el-tabs v-model="detailTab">
          <el-tab-pane label="基础信息" name="base">
            <div class="detail-card">
              <h3 class="detail-card__title">{{ isSupplierRoute ? '订单信息' : '订单头字段' }}</h3>
              <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
                <el-descriptions-item label="订单号">{{ formatEmpty(detail.orderNo) }}</el-descriptions-item>
                <el-descriptions-item label="申请标题">{{ formatEmpty(detail.requestTitle) }}</el-descriptions-item>
                <el-descriptions-item v-if="!isSupplierRoute" label="供应商名称">{{ formatEmpty(detail.supplierName) }}</el-descriptions-item>
                <el-descriptions-item label="采购员">{{ formatEmpty(detail.purchaserName) }}</el-descriptions-item>
                <el-descriptions-item label="计划到货日期">{{ formatDate(detail.planDate) }}</el-descriptions-item>
                <el-descriptions-item label="供应商承诺日期">{{ formatDate(detail.supplierDate) }}</el-descriptions-item>
                <el-descriptions-item label="确认时间">{{ formatDateTime(detail.confirmTime) }}</el-descriptions-item>
                <el-descriptions-item label="总金额">{{ formatEmpty(detail.totalAmount) }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag
                    class="status-tag"
                    :type="getOptionType(purchaseOrderStatusOptions, detail.status)"
                    effect="plain"
                  >
                    {{ getOptionLabel(purchaseOrderStatusOptions, detail.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item :label="isSupplierRoute ? '反馈备注' : '供应商备注'">{{ formatEmpty(detail.supplierNote) }}</el-descriptions-item>
                <el-descriptions-item v-if="!isSupplierRoute" label="关闭时间">{{ formatDateTime(detail.closeTime) }}</el-descriptions-item>
                <el-descriptions-item v-if="!isSupplierRoute" label="关闭原因">{{ formatEmpty(detail.closeReason) }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
                <el-descriptions-item v-if="!isSupplierRoute" label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
                <el-descriptions-item v-if="!isSupplierRoute" label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isSupplierRoute ? '物料明细' : '订单明细'" name="items">
            <section class="content-panel">
              <div class="panel-toolbar">
                <div>
                  <div class="page-title" style="font-size: 16px">{{ isSupplierRoute ? '物料明细' : '订单明细字段' }}</div>
                  <div class="page-desc">
                    {{ isSupplierRoute ? '核对本次订单的物料、数量、价格和后续到货入库进度' : '保留数量、单价、金额、到货和入库累计值' }}
                  </div>
                </div>
              </div>
              <div class="table-wrap">
                <el-table :data="detailItems" table-layout="fixed">
                  <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="unit" label="单位" width="88" />
                  <el-table-column prop="orderNumber" label="订单数量" min-width="110" />
                  <el-table-column prop="unitPrice" label="单价" min-width="110" />
                  <el-table-column prop="lineAmount" label="行金额" min-width="110" />
                  <el-table-column prop="arrivedNumber" label="已到货" min-width="110" />
                  <el-table-column prop="inboundNumber" label="已入库" min-width="110" />
                  <el-table-column label="备注" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
                  </el-table-column>
                  <el-table-column v-if="!isSupplierRoute" label="创建时间" min-width="168">
                    <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                  </el-table-column>
                  <el-table-column v-if="!isSupplierRoute" label="更新时间" min-width="168">
                    <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
                  </el-table-column>
                  <el-table-column v-if="canManage" label="操作" width="120" fixed="right">
                    <template #default="{ row }">
                      <div class="table-actions">
                        <el-button link type="primary" @click="openEditItemDialog(row)">编辑</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </section>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-dialog
      v-model="itemDialogVisible"
      title="编辑订单明细"
      width="620px"
      @opened="refreshEditingItem"
      @closed="editingItem = null"
    >
      <el-form ref="itemFormRef" v-loading="itemFormLoading" :model="itemForm" :rules="itemFormRules" label-width="108px">
        <div class="detail-card">
          <h3 class="detail-card__title">基础字段</h3>
          <el-descriptions v-if="editingItem" class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="物料编码">{{ formatEmpty(editingItem.materialCode) }}</el-descriptions-item>
            <el-descriptions-item label="物料名称">{{ formatEmpty(editingItem.materialName) }}</el-descriptions-item>
            <el-descriptions-item label="规格型号">{{ formatEmpty(editingItem.specification) }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ formatEmpty(editingItem.unit) }}</el-descriptions-item>
            <el-descriptions-item label="订单数量">{{ formatEmpty(editingItem.orderNumber) }}</el-descriptions-item>
            <el-descriptions-item label="已到货">{{ formatEmpty(editingItem.arrivedNumber) }}</el-descriptions-item>
            <el-descriptions-item label="已入库">{{ formatEmpty(editingItem.inboundNumber) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(editingItem.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(editingItem.updateTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <el-form-item label="单价" prop="unitPrice" style="margin-top: 18px">
          <el-input-number v-model="itemForm.unitPrice" :min="0.01" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="itemDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="itemFormLoading" @click="submitItemForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.supplier-status-tabs {
  margin-bottom: 16px;
}

.confirm-summary {
  margin-bottom: 16px;
}

.confirm-items {
  margin-bottom: 20px;
}

.confirm-items__title {
  margin-bottom: 10px;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.form-embed-panel {
  margin-top: 18px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
