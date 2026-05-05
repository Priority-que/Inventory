<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  addPurchaseOrderApi,
  cancelPurchaseOrderApi,
  closePurchaseOrderApi,
  getPurchaseOrderByIdApi,
  getPurchaseOrderItemByIdApi,
  getPurchaseOrderItemsByOrderIdApi,
  getPurchaseOrderPageApi,
  updatePurchaseOrderApi,
  updatePurchaseOrderItemApi,
  type PurchaseOrderDTO,
  type PurchaseOrderItemCreateDTO,
  type PurchaseOrderItemDTO,
  type PurchaseOrderItemVO,
  type PurchaseOrderVO,
} from '@/api/purchaseOrder'
import { getPurchaseRequestItemsByRequestIdApi, type PurchaseRequestItemVO } from '@/api/purchaseRequest'
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
const authStore = useAuthStore()

const canManage = computed(() => route.path.startsWith('/purchaser'))
const pageTitle = computed(() => String(route.meta.title || '采购订单'))
const pageSubtitle = computed(() =>
  canManage.value ? '维护采购订单头信息、订单明细与执行状态' : '查看采购订单、明细履约与上下游流转字段',
)

const loading = ref(false)
const tableData = ref<PurchaseOrderVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  requestTitle: '',
  supplierName: '',
  purchaseName: '',
  planDateBegin: '',
  planDateEnd: '',
  status: '',
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
  purchaserId: authStore.user?.id,
  planDate: '',
  supplierDate: '',
  status: 'WAIT_CONFIRM',
  supplierNote: '',
  closeReason: '',
  remark: '',
  items: [],
})
const requestItemRows = ref<DraftOrderItem[]>([])

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

const formRules: FormRules<PurchaseOrderDTO> = {
  requestId: [{ required: true, message: '请输入采购申请 ID', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请输入供应商 ID', trigger: 'blur' }],
  planDate: [{ required: true, message: '请选择计划到货日期', trigger: 'change' }],
}

const itemFormRules: FormRules<PurchaseOrderItemDTO> = {
  id: [{ required: true, message: '缺少订单明细 ID', trigger: 'change' }],
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
    const result = await getPurchaseOrderPageApi(query)
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
    status: '',
  })
  planDateRange.value = []
  loadData()
}

function resetForm() {
  Object.assign(orderForm, {
    id: undefined,
    orderNo: '',
    requestId: undefined,
    supplierId: undefined,
    purchaserId: authStore.user?.id,
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
    ElMessage.warning('请先输入采购申请 ID')
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
          status: item.status,
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
  return canManage.value && !['COMPLETED', 'CLOSED', 'CANCELLED'].includes(row.status || '')
}

function canCancelRow(row: PurchaseOrderVO) {
  return canManage.value && !['COMPLETED', 'CLOSED', 'CANCELLED'].includes(row.status || '')
}

function canCloseRow(row: PurchaseOrderVO) {
  return canManage.value && !['COMPLETED', 'CLOSED', 'CANCELLED'].includes(row.status || '')
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
        新增采购订单
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="订单号">
            <el-input v-model="query.orderNo" clearable placeholder="输入采购订单号" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="申请标题">
            <el-input v-model="query.requestTitle" clearable placeholder="输入申请标题" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="供应商名称">
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
          <el-form-item label="状态">
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
          <el-table-column prop="id" label="ID" width="88" fixed="left" />
          <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="requestId" label="申请 ID" width="100" />
          <el-table-column prop="requestTitle" label="申请标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="supplierId" label="供应商 ID" width="110" />
          <el-table-column prop="supplierName" label="供应商名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="purchaserId" label="采购员 ID" width="110" />
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
          <el-table-column label="供应商备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.supplierNote) }}</template>
          </el-table-column>
          <el-table-column label="关闭时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.closeTime) }}</template>
          </el-table-column>
          <el-table-column label="关闭原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.closeReason) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column prop="deleted" label="删除标记" width="96" />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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
      :title="dialogMode === 'create' ? '新增采购订单' : '编辑采购订单'"
      width="1160px"
      @closed="resetForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="orderForm" :rules="formRules" label-width="120px">
        <div class="dialog-grid">
          <el-form-item label="采购申请 ID" prop="requestId">
            <el-input-number
              v-model="orderForm.requestId"
              :disabled="dialogMode === 'edit'"
              :min="1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="供应商 ID" prop="supplierId">
            <el-input-number v-model="orderForm.supplierId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="采购员 ID">
            <el-input :model-value="String(orderForm.purchaserId || '')" disabled />
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
          <el-form-item label="供应商承诺日期">
            <el-date-picker
              v-model="orderForm.supplierDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
        </div>
        <el-form-item label="供应商备注">
          <el-input v-model="orderForm.supplierNote" :rows="2" type="textarea" placeholder="填写供应商备注" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" :rows="2" type="textarea" placeholder="填写备注" />
        </el-form-item>

        <section class="content-panel form-embed-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">订单明细字段</div>
              <div class="page-desc">
                创建时保留 `requestItemId`、`unitPrice`、`remark`、`status`；编辑时展示现有明细
              </div>
            </div>
            <el-button v-if="dialogMode === 'create'" :loading="requestItemsLoading" @click="loadRequestItems">
              <el-icon><Refresh /></el-icon>
              加载申请明细
            </el-button>
          </div>
          <div class="table-wrap">
            <el-table v-loading="requestItemsLoading" :data="requestItemRows" table-layout="fixed">
              <el-table-column prop="requestItemId" label="申请明细 ID" width="120" />
              <el-table-column prop="materialId" label="物料 ID" width="100" />
              <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="88" />
              <el-table-column label="申请数量 / 订单数量" min-width="140">
                <template #default="{ row }">
                  {{ formatEmpty(row.requestNumber ?? row.orderNumber) }}
                </template>
              </el-table-column>
              <el-table-column prop="sortNumber" label="排序号" width="90" />
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
              <el-table-column label="状态" min-width="120">
                <template #default="{ row }">
                  <el-input v-if="dialogMode === 'create'" v-model="row.status" placeholder="保留原字段名 status" />
                  <span v-else>{{ formatEmpty(row.status) }}</span>
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

        <el-collapse>
          <el-collapse-item title="系统字段与只读字段" name="order-system">
            <el-descriptions class="detail-descriptions" :column="2" border>
              <el-descriptions-item label="ID">{{ formatEmpty(orderForm.id) }}</el-descriptions-item>
              <el-descriptions-item label="订单号">{{ formatEmpty(orderForm.orderNo) }}</el-descriptions-item>
              <el-descriptions-item label="确认时间">{{ formatDateTime(orderForm.confirmTime) }}</el-descriptions-item>
              <el-descriptions-item label="总金额">{{ formatEmpty(orderForm.totalAmount) }}</el-descriptions-item>
              <el-descriptions-item label="关闭时间">{{ formatDateTime(orderForm.closeTime) }}</el-descriptions-item>
              <el-descriptions-item label="关闭原因">{{ formatEmpty(orderForm.closeReason) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(orderForm.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDateTime(orderForm.updateTime) }}</el-descriptions-item>
              <el-descriptions-item label="删除标记">{{ formatEmpty(orderForm.deleted) }}</el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="pageTitle + '详情'" size="1180px">
      <div v-loading="detailLoading" class="detail-section">
        <el-tabs v-model="detailTab">
          <el-tab-pane label="基础信息" name="base">
            <div class="detail-card">
              <h3 class="detail-card__title">订单头字段</h3>
              <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
                <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
                <el-descriptions-item label="订单号">{{ formatEmpty(detail.orderNo) }}</el-descriptions-item>
                <el-descriptions-item label="申请 ID">{{ formatEmpty(detail.requestId) }}</el-descriptions-item>
                <el-descriptions-item label="申请标题">{{ formatEmpty(detail.requestTitle) }}</el-descriptions-item>
                <el-descriptions-item label="供应商 ID">{{ formatEmpty(detail.supplierId) }}</el-descriptions-item>
                <el-descriptions-item label="供应商名称">{{ formatEmpty(detail.supplierName) }}</el-descriptions-item>
                <el-descriptions-item label="采购员 ID">{{ formatEmpty(detail.purchaserId) }}</el-descriptions-item>
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
                <el-descriptions-item label="供应商备注">{{ formatEmpty(detail.supplierNote) }}</el-descriptions-item>
                <el-descriptions-item label="关闭时间">{{ formatDateTime(detail.closeTime) }}</el-descriptions-item>
                <el-descriptions-item label="关闭原因">{{ formatEmpty(detail.closeReason) }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
                <el-descriptions-item label="删除标记">{{ formatEmpty(detail.deleted) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <el-tab-pane label="订单明细" name="items">
            <section class="content-panel">
              <div class="panel-toolbar">
                <div>
                  <div class="page-title" style="font-size: 16px">订单明细字段</div>
                  <div class="page-desc">保留数量、单价、金额、到货和入库累计值</div>
                </div>
              </div>
              <div class="table-wrap">
                <el-table :data="detailItems" table-layout="fixed">
                  <el-table-column prop="id" label="ID" width="88" />
                  <el-table-column prop="orderId" label="订单 ID" width="100" />
                  <el-table-column prop="requestItemId" label="申请明细 ID" width="120" />
                  <el-table-column prop="materialId" label="物料 ID" width="100" />
                  <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="unit" label="单位" width="88" />
                  <el-table-column prop="orderNumber" label="订单数量" min-width="110" />
                  <el-table-column prop="unitPrice" label="单价" min-width="110" />
                  <el-table-column prop="lineAmount" label="行金额" min-width="110" />
                  <el-table-column prop="arrivedNumber" label="已到货" min-width="110" />
                  <el-table-column prop="inboundNumber" label="已入库" min-width="110" />
                  <el-table-column prop="sortNumber" label="排序号" width="90" />
                  <el-table-column label="备注" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
                  </el-table-column>
                  <el-table-column label="创建时间" min-width="168">
                    <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                  </el-table-column>
                  <el-table-column label="更新时间" min-width="168">
                    <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
                  </el-table-column>
                  <el-table-column prop="deleted" label="删除标记" width="96" />
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
            <el-descriptions-item label="明细 ID">{{ editingItem.id }}</el-descriptions-item>
            <el-descriptions-item label="订单 ID">{{ formatEmpty(editingItem.orderId) }}</el-descriptions-item>
            <el-descriptions-item label="申请明细 ID">{{ formatEmpty(editingItem.requestItemId) }}</el-descriptions-item>
            <el-descriptions-item label="物料 ID">{{ formatEmpty(editingItem.materialId) }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ formatEmpty(editingItem.materialCode) }}</el-descriptions-item>
            <el-descriptions-item label="物料名称">{{ formatEmpty(editingItem.materialName) }}</el-descriptions-item>
            <el-descriptions-item label="规格型号">{{ formatEmpty(editingItem.specification) }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ formatEmpty(editingItem.unit) }}</el-descriptions-item>
            <el-descriptions-item label="订单数量">{{ formatEmpty(editingItem.orderNumber) }}</el-descriptions-item>
            <el-descriptions-item label="已到货">{{ formatEmpty(editingItem.arrivedNumber) }}</el-descriptions-item>
            <el-descriptions-item label="已入库">{{ formatEmpty(editingItem.inboundNumber) }}</el-descriptions-item>
            <el-descriptions-item label="排序号">{{ formatEmpty(editingItem.sortNumber) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(editingItem.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(editingItem.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="删除标记">{{ formatEmpty(editingItem.deleted) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <el-form-item label="明细 ID" prop="id" style="margin-top: 18px">
          <el-input :model-value="String(itemForm.id || '')" disabled />
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
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
.form-embed-panel {
  margin-top: 18px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
