<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { addArrivalApi, getArrivalByIdApi, getArrivalPageApi, type ArrivalDTO, type ArrivalItemVO, type ArrivalVO } from '@/api/arrival'
import {
  getPurchaseOrderItemsByOrderIdApi,
  getPurchaseOrderPageApi,
  type PurchaseOrderItemVO,
  type PurchaseOrderVO,
} from '@/api/purchaseOrder'
import { getWarehousePageApi, type WarehousePageVO } from '@/api/warehouse'
import { arrivalStatusOptions, getOptionLabel, getOptionType, purchaseOrderStatusOptions } from '@/constants/business'
import { formatDate, formatEmpty } from '@/utils/format'

interface DraftArrivalItem {
  orderItemId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  specification?: string
  unit?: string
  orderNumber?: number
  arrivedNumber?: number
  inboundNumber?: number
  sortNumber?: number
  arrivalNumber?: number
  qualifiedNumber?: number
  unqualifiedNumber?: number
  abnormalNote?: string
  remark?: string
}

const route = useRoute()
const router = useRouter()

const canManage = computed(() => route.path.startsWith('/warehouse'))
const pageTitle = computed(() => String(route.meta.title || '到货管理'))
const pageSubtitle = computed(() =>
  canManage.value ? '登记采购订单到货、质检数量和异常情况' : '查看到货执行结果与异常到货情况',
)
const arrivalEmptyText = computed(() => {
  if (query.status === 'ABNORMAL') {
    return '暂无异常到货记录'
  }
  if (query.status === 'NORMAL') {
    return '暂无正常到货记录'
  }
  return canManage.value ? '暂无到货记录，可登记采购订单到货' : '暂无到货跟踪记录'
})

const loading = ref(false)
const tableData = ref<ArrivalVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  arrivalNo: '',
  orderNo: '',
  warehouseName: '',
  arrivalDateBegin: '',
  arrivalDateEnd: '',
  status: '',
})
const arrivalDateRange = ref<string[]>([])

const dialogVisible = ref(false)
const formLoading = ref(false)
const orderItemsLoading = ref(false)
const formRef = ref<FormInstance>()
const arrivalForm = reactive<ArrivalDTO>({
  orderId: undefined,
  warehouseId: undefined,
  arrivalDate: '',
  remark: '',
  items: [],
})
const arrivalItems = ref<DraftArrivalItem[]>([])
const selectedOrder = ref<PurchaseOrderVO | null>(null)
const selectedWarehouse = ref<WarehousePageVO | null>(null)

const orderDialogVisible = ref(false)
const orderLoading = ref(false)
const orderRows = ref<PurchaseOrderVO[]>([])
const orderTotal = ref(0)
const orderQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  requestTitle: '',
  supplierName: '',
  status: 'IN_PROGRESS',
})

const warehouseDialogVisible = ref(false)
const warehouseLoading = ref(false)
const warehouseRows = ref<WarehousePageVO[]>([])
const warehouseTotal = ref(0)
const warehouseQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  code: '',
  name: '',
  managerName: '',
  status: 'ENABLED',
})
const arrivalOrderStatusOptions = purchaseOrderStatusOptions.filter((item) =>
  ['IN_PROGRESS', 'PARTIAL_ARRIVAL'].includes(String(item.value)),
)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ArrivalVO | null>(null)
const detailItems = ref<ArrivalItemVO[]>([])

const formRules: FormRules<ArrivalDTO> = {
  orderId: [{ required: true, message: '请选择采购订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  arrivalDate: [{ required: true, message: '请选择到货日期', trigger: 'change' }],
}

function syncArrivalDateRange() {
  query.arrivalDateBegin = arrivalDateRange.value[0] || ''
  query.arrivalDateEnd = arrivalDateRange.value[1] || ''
}

function applyRouteQuery() {
  if (typeof route.query.status === 'string') {
    query.status = route.query.status
  }
  if (typeof route.query.orderStatus === 'string') {
    orderQuery.status = route.query.orderStatus
  }
}

async function loadData() {
  syncArrivalDateRange()
  loading.value = true
  try {
    const result = await getArrivalPageApi(query)
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
    arrivalNo: '',
    orderNo: '',
    warehouseName: '',
    arrivalDateBegin: '',
    arrivalDateEnd: '',
    status: '',
  })
  arrivalDateRange.value = []
  loadData()
}

function resetForm() {
  Object.assign(arrivalForm, {
    id: undefined,
    orderId: undefined,
    warehouseId: undefined,
    arrivalDate: '',
    remark: '',
    items: [],
    deleted: undefined,
  })
  arrivalItems.value = []
  selectedOrder.value = null
  selectedWarehouse.value = null
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
  loadSelectableOrders()
  loadSelectableWarehouses()
}

function getRemainingArrivalNumber(item: DraftArrivalItem) {
  return Math.max(Number(item.orderNumber || 0) - Number(item.arrivedNumber || 0), 0)
}

async function loadSelectableOrders() {
  orderLoading.value = true
  try {
    const result = await getPurchaseOrderPageApi(orderQuery)
    orderRows.value = result.records
    orderTotal.value = result.total
  } finally {
    orderLoading.value = false
  }
}

function openOrderDialog() {
  orderDialogVisible.value = true
  if (!orderRows.value.length) {
    loadSelectableOrders()
  }
}

function handleOrderSearch() {
  orderQuery.pageNum = 1
  loadSelectableOrders()
}

function handleOrderReset() {
  Object.assign(orderQuery, {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    requestTitle: '',
    supplierName: '',
    status: 'IN_PROGRESS',
  })
  loadSelectableOrders()
}

async function selectOrder(row: PurchaseOrderVO) {
  selectedOrder.value = row
  arrivalForm.orderId = row.id
  arrivalItems.value = []
  orderDialogVisible.value = false
  formRef.value?.validateField('orderId')
  await loadOrderItems()
}

function handleOrderSizeChange(size: number) {
  orderQuery.pageSize = size
  orderQuery.pageNum = 1
  loadSelectableOrders()
}

function handleOrderCurrentChange(page: number) {
  orderQuery.pageNum = page
  loadSelectableOrders()
}

async function loadSelectableWarehouses() {
  warehouseLoading.value = true
  try {
    const result = await getWarehousePageApi(warehouseQuery)
    warehouseRows.value = result.records
    warehouseTotal.value = result.total
  } finally {
    warehouseLoading.value = false
  }
}

function openWarehouseDialog() {
  warehouseDialogVisible.value = true
  if (!warehouseRows.value.length) {
    loadSelectableWarehouses()
  }
}

function handleWarehouseSearch() {
  warehouseQuery.pageNum = 1
  loadSelectableWarehouses()
}

function handleWarehouseReset() {
  Object.assign(warehouseQuery, {
    pageNum: 1,
    pageSize: 10,
    code: '',
    name: '',
    managerName: '',
    status: 'ENABLED',
  })
  loadSelectableWarehouses()
}

function selectWarehouse(row: WarehousePageVO) {
  selectedWarehouse.value = row
  arrivalForm.warehouseId = row.id
  warehouseDialogVisible.value = false
  formRef.value?.validateField('warehouseId')
}

function handleWarehouseSizeChange(size: number) {
  warehouseQuery.pageSize = size
  warehouseQuery.pageNum = 1
  loadSelectableWarehouses()
}

function handleWarehouseCurrentChange(page: number) {
  warehouseQuery.pageNum = page
  loadSelectableWarehouses()
}

function mapOrderItems(items: PurchaseOrderItemVO[]) {
  arrivalItems.value = items
    .map((item) => {
      const orderNumber = Number(item.orderNumber || 0)
      const arrivedNumber = Number(item.arrivedNumber || 0)
      const remaining = Math.max(orderNumber - arrivedNumber, 0)

      return {
        orderItemId: item.id,
        materialId: item.materialId,
        materialCode: item.materialCode,
        materialName: item.materialName,
        specification: item.specification,
        unit: item.unit,
        orderNumber: item.orderNumber,
        arrivedNumber: item.arrivedNumber,
        inboundNumber: item.inboundNumber,
        sortNumber: item.sortNumber,
        arrivalNumber: remaining,
        qualifiedNumber: remaining,
        unqualifiedNumber: 0,
        abnormalNote: '',
        remark: '',
      }
    })
    .filter((item) => getRemainingArrivalNumber(item) > 0)
}

async function loadOrderItems() {
  if (!arrivalForm.orderId) {
    ElMessage.warning('请先选择采购订单')
    return
  }

  orderItemsLoading.value = true
  try {
    const items = await getPurchaseOrderItemsByOrderIdApi(arrivalForm.orderId)
    mapOrderItems(items)
    if (!arrivalItems.value.length) {
      ElMessage.warning('该采购订单暂无剩余可登记到货数量')
      return
    }
    ElMessage.success(`已加载 ${arrivalItems.value.length} 条可到货明细`)
  } finally {
    orderItemsLoading.value = false
  }
}

async function submitForm() {
  await formRef.value?.validate()
  const submittingItems = arrivalItems.value.filter((item) => Number(item.arrivalNumber || 0) > 0)
  if (!submittingItems.length) {
    ElMessage.warning('请至少维护一条本次到货数量大于 0 的明细')
    return
  }
  for (const item of submittingItems) {
    const arrivalNumber = Number(item.arrivalNumber || 0)
    const qualifiedNumber = Number(item.qualifiedNumber || 0)
    const unqualifiedNumber = Number(item.unqualifiedNumber || 0)
    if (arrivalNumber > getRemainingArrivalNumber(item)) {
      ElMessage.warning(`${item.materialName || item.materialCode || '物料'} 的到货数量超过剩余可到货数量`)
      return
    }
    if (Math.abs(qualifiedNumber + unqualifiedNumber - arrivalNumber) > 0.000001) {
      ElMessage.warning(`${item.materialName || item.materialCode || '物料'} 的合格数量与不合格数量之和必须等于到货数量`)
      return
    }
  }

  formLoading.value = true
  try {
    await addArrivalApi({
      ...arrivalForm,
      items: submittingItems.map((item) => ({
        orderItemId: item.orderItemId,
        arrivalNumber: item.arrivalNumber,
        qualifiedNumber: item.qualifiedNumber,
        unqualifiedNumber: item.unqualifiedNumber,
        abnormalNote: item.abnormalNote,
        remark: item.remark,
      })),
    })
    ElMessage.success('到货单已登记，可继续生成入库单')
    dialogVisible.value = false
    loadData()
    if (canManage.value) {
      try {
        await ElMessageBox.confirm('到货单已登记，可继续生成入库单。是否前往入库管理？', '下一步处理', {
          type: 'success',
          confirmButtonText: '去生成入库单',
          cancelButtonText: '稍后处理',
        })
        router.push({ path: '/warehouse/inbound', query: { todo: 'pendingArrival' } })
      } catch {
        // 用户选择稍后处理时不打断当前流程
      }
    }
  } finally {
    formLoading.value = false
  }
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    const result = await getArrivalByIdApi(id)
    detail.value = result
    detailItems.value = result.items || []
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: ArrivalVO) {
  detailVisible.value = true
  await loadDetail(row.id)
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

onMounted(() => {
  applyRouteQuery()
  loadData()
})
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
        登记到货
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="到货单号">
            <el-input v-model="query.arrivalNo" clearable placeholder="输入到货单号" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="订单号">
            <el-input v-model="query.orderNo" clearable placeholder="输入采购订单号" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="仓库名称">
            <el-input
              v-model="query.warehouseName"
              clearable
              placeholder="输入仓库名称"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option v-for="item in arrivalStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="到货日期">
            <el-date-picker
              v-model="arrivalDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
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
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed" :empty-text="arrivalEmptyText">
          <el-table-column prop="arrivalNo" label="到货单号" min-width="160" fixed="left" show-overflow-tooltip />
          <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="到货日期" min-width="128">
            <template #default="{ row }">{{ formatDate(row.arrivalDate) }}</template>
          </el-table-column>
          <el-table-column prop="arrivalNumber" label="到货数量" min-width="110" />
          <el-table-column prop="qualifiedNumber" label="合格数量" min-width="110" />
          <el-table-column prop="unqualifiedNumber" label="不合格数量" min-width="120" />
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(arrivalStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(arrivalStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="异常说明" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.abnormalNote) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="dialogVisible" title="登记到货" width="1160px" @closed="resetForm">
      <el-form ref="formRef" v-loading="formLoading" :model="arrivalForm" :rules="formRules" label-width="108px">
        <div class="dialog-grid">
          <el-form-item label="采购订单" prop="orderId">
            <el-input
              :model-value="selectedOrder ? `${selectedOrder.orderNo || ''} / ${selectedOrder.requestTitle || ''}` : ''"
              readonly
              placeholder="选择可登记到货的采购订单"
            >
              <template #append>
                <el-button @click="openOrderDialog">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="到货仓库" prop="warehouseId">
            <el-input
              :model-value="selectedWarehouse ? `${selectedWarehouse.code || ''} / ${selectedWarehouse.name || ''}` : ''"
              readonly
              placeholder="选择到货仓库"
            >
              <template #append>
                <el-button @click="openWarehouseDialog">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="到货日期" prop="arrivalDate">
            <el-date-picker
              v-model="arrivalForm.arrivalDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择到货日期"
            />
          </el-form-item>
        </div>
        <el-descriptions v-if="selectedOrder || selectedWarehouse" class="detail-descriptions" :column="2" border>
          <el-descriptions-item label="订单状态">
            <el-tag
              v-if="selectedOrder"
              class="status-tag"
              :type="getOptionType(purchaseOrderStatusOptions, selectedOrder.status)"
              effect="plain"
            >
              {{ getOptionLabel(purchaseOrderStatusOptions, selectedOrder.status) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="供应商">{{ formatEmpty(selectedOrder?.supplierName) }}</el-descriptions-item>
          <el-descriptions-item label="采购申请">{{ formatEmpty(selectedOrder?.requestTitle) }}</el-descriptions-item>
          <el-descriptions-item label="仓库负责人">{{ formatEmpty(selectedWarehouse?.managerName) }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="备注">
          <el-input v-model="arrivalForm.remark" :rows="2" type="textarea" placeholder="填写到货备注" />
        </el-form-item>

        <section class="content-panel form-embed-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">到货明细</div>
              <div class="page-desc">根据订单剩余未到货数量登记本次到货和质检结果</div>
            </div>
            <el-button :loading="orderItemsLoading" @click="loadOrderItems">
              <el-icon><Refresh /></el-icon>
              重新加载明细
            </el-button>
          </div>
          <div class="table-wrap">
            <el-table
              v-loading="orderItemsLoading"
              :data="arrivalItems"
              table-layout="fixed"
              empty-text="请选择采购订单后加载可到货明细"
            >
              <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="88" />
              <el-table-column label="订单数量" width="100">
                <template #default="{ row }">{{ formatEmpty(row.orderNumber) }}</template>
              </el-table-column>
              <el-table-column label="已到货" width="100">
                <template #default="{ row }">{{ formatEmpty(row.arrivedNumber) }}</template>
              </el-table-column>
              <el-table-column label="已入库" width="100">
                <template #default="{ row }">{{ formatEmpty(row.inboundNumber) }}</template>
              </el-table-column>
              <el-table-column label="剩余可到货" width="120">
                <template #default="{ row }">{{ formatEmpty(getRemainingArrivalNumber(row)) }}</template>
              </el-table-column>
              <el-table-column label="到货数量" min-width="132">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.arrivalNumber"
                    :min="0"
                    :max="getRemainingArrivalNumber(row)"
                    controls-position="right"
                  />
                </template>
              </el-table-column>
              <el-table-column label="合格数量" min-width="132">
                <template #default="{ row }">
                  <el-input-number v-model="row.qualifiedNumber" :min="0" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="不合格数量" min-width="132">
                <template #default="{ row }">
                  <el-input-number v-model="row.unqualifiedNumber" :min="0" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="异常说明" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.abnormalNote" placeholder="填写异常说明" />
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.remark" placeholder="填写备注" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">登记到货</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="orderDialogVisible" title="选择采购订单" width="980px">
      <div class="filter-section">
        <el-form :model="orderQuery" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="订单号">
            <el-input v-model="orderQuery.orderNo" clearable placeholder="输入订单号" @keyup.enter="handleOrderSearch" />
          </el-form-item>
          <el-form-item label="采购申请">
            <el-input
              v-model="orderQuery.requestTitle"
              clearable
              placeholder="输入采购申请"
              @keyup.enter="handleOrderSearch"
            />
          </el-form-item>
          <el-form-item label="供应商">
            <el-input
              v-model="orderQuery.supplierName"
              clearable
              placeholder="输入供应商"
              @keyup.enter="handleOrderSearch"
            />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="orderQuery.status" placeholder="选择状态" @change="handleOrderSearch">
              <el-option
                v-for="item in arrivalOrderStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleOrderReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleOrderSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="table-wrap">
        <el-table
          v-loading="orderLoading"
          :data="orderRows"
          row-key="id"
          table-layout="fixed"
          empty-text="暂无可登记到货的采购订单"
        >
          <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="requestTitle" label="采购申请" min-width="180" show-overflow-tooltip />
          <el-table-column prop="supplierName" label="供应商" min-width="160" show-overflow-tooltip />
          <el-table-column prop="purchaserName" label="采购员" min-width="120" show-overflow-tooltip />
          <el-table-column label="计划到货日期" min-width="128">
            <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(purchaseOrderStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(purchaseOrderStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectOrder(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="orderQuery.pageNum"
          v-model:page-size="orderQuery.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="orderTotal"
          @size-change="handleOrderSizeChange"
          @current-change="handleOrderCurrentChange"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="warehouseDialogVisible" title="选择到货仓库" width="900px">
      <div class="filter-section">
        <el-form :model="warehouseQuery" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="仓库编码">
            <el-input
              v-model="warehouseQuery.code"
              clearable
              placeholder="输入仓库编码"
              @keyup.enter="handleWarehouseSearch"
            />
          </el-form-item>
          <el-form-item label="仓库名称">
            <el-input
              v-model="warehouseQuery.name"
              clearable
              placeholder="输入仓库名称"
              @keyup.enter="handleWarehouseSearch"
            />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input
              v-model="warehouseQuery.managerName"
              clearable
              placeholder="输入负责人"
              @keyup.enter="handleWarehouseSearch"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleWarehouseReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleWarehouseSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="table-wrap">
        <el-table
          v-loading="warehouseLoading"
          :data="warehouseRows"
          row-key="id"
          table-layout="fixed"
          empty-text="暂无可用仓库"
        >
          <el-table-column prop="code" label="仓库编码" min-width="140" show-overflow-tooltip />
          <el-table-column prop="name" label="仓库名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
          <el-table-column prop="managerName" label="负责人" min-width="120" show-overflow-tooltip />
          <el-table-column prop="managerPhone" label="联系电话" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectWarehouse(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="warehouseQuery.pageNum"
          v-model:page-size="warehouseQuery.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="warehouseTotal"
          @size-change="handleWarehouseSizeChange"
          @current-change="handleWarehouseCurrentChange"
        />
      </div>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="pageTitle + '详情'" size="1180px">
      <div v-loading="detailLoading" class="detail-section">
        <div class="detail-card">
          <h3 class="detail-card__title">到货信息</h3>
          <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="到货单号">{{ formatEmpty(detail.arrivalNo) }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ formatEmpty(detail.orderNo) }}</el-descriptions-item>
            <el-descriptions-item label="仓库名称">{{ formatEmpty(detail.warehouseName) }}</el-descriptions-item>
            <el-descriptions-item label="到货日期">{{ formatDate(detail.arrivalDate) }}</el-descriptions-item>
            <el-descriptions-item label="到货数量">{{ formatEmpty(detail.arrivalNumber) }}</el-descriptions-item>
            <el-descriptions-item label="合格数量">{{ formatEmpty(detail.qualifiedNumber) }}</el-descriptions-item>
            <el-descriptions-item label="不合格数量">{{ formatEmpty(detail.unqualifiedNumber) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag class="status-tag" :type="getOptionType(arrivalStatusOptions, detail.status)" effect="plain">
                {{ getOptionLabel(arrivalStatusOptions, detail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="异常说明">{{ formatEmpty(detail.abnormalNote) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <section class="content-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">到货明细</div>
              <div class="page-desc">查看本次到货的物料、数量和异常说明</div>
            </div>
          </div>
          <div class="table-wrap">
            <el-table :data="detailItems" table-layout="fixed" empty-text="暂无到货明细">
              <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="88" />
              <el-table-column prop="arrivalNumber" label="到货数量" min-width="110" />
              <el-table-column prop="qualifiedNumber" label="合格数量" min-width="110" />
              <el-table-column prop="unqualifiedNumber" label="不合格数量" min-width="120" />
              <el-table-column label="异常说明" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ formatEmpty(row.abnormalNote) }}</template>
              </el-table-column>
              <el-table-column label="备注" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </div>
    </el-drawer>
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
