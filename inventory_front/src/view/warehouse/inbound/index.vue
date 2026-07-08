<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addInboundApi,
  cancelInboundApi,
  confirmInboundApi,
  getInboundByIdApi,
  getInboundPageApi,
  type InboundDTO,
  type InboundItemVO,
  type InboundVO,
} from '@/api/inbound'
import { getArrivalByIdApi, getArrivalPageApi, type ArrivalItemVO, type ArrivalVO } from '@/api/arrival'
import { arrivalStatusOptions, getOptionLabel, getOptionType, inboundStatusOptions } from '@/constants/business'
import { formatDate, formatDateTime, formatEmpty } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const canManage = computed(() => route.path.startsWith('/warehouse'))
const pageTitle = computed(() => String(route.meta.title || '入库管理'))
const pageSubtitle = computed(() =>
  canManage.value ? '生成入库单、确认入库并更新库存' : '查看入库单状态、仓库流转和明细结果',
)
const inboundEmptyText = computed(() => {
  if (query.status === 'PENDING') {
    return '暂无待确认入库单'
  }
  if (query.status === 'COMPLETED') {
    return '暂无已完成入库单'
  }
  if (query.status === 'CANCELLED') {
    return '暂无已取消入库单'
  }
  if (query.status === 'ABNORMAL') {
    return '暂无异常入库单'
  }
  return '暂无入库单'
})

const loading = ref(false)
const tableData = ref<InboundVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  inboundNo: '',
  arrivalNo: '',
  orderNo: '',
  warehouseName: '',
  status: '',
  inboundTimeBegin: '',
  inboundTimeEnd: '',
})
const inboundTimeRange = ref<string[]>([])

const dialogVisible = ref(false)
const formLoading = ref(false)
const previewLoading = ref(false)
const formRef = ref<FormInstance>()
const inboundForm = reactive<InboundDTO>({
  arrivalId: undefined,
  remark: '',
})
const previewArrival = ref<ArrivalVO | null>(null)
const previewArrivalItems = ref<ArrivalItemVO[]>([])
const selectedArrival = ref<ArrivalVO | null>(null)

const arrivalDialogVisible = ref(false)
const arrivalLoading = ref(false)
const arrivalRows = ref<ArrivalVO[]>([])
const arrivalTotal = ref(0)
const arrivalQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  arrivalNo: '',
  orderNo: '',
  warehouseName: '',
  status: '',
  pendingInboundOnly: true,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<InboundVO | null>(null)
const detailItems = ref<InboundItemVO[]>([])

const formRules: FormRules<InboundDTO> = {
  arrivalId: [{ required: true, message: '请选择到货单', trigger: 'change' }],
}

function syncInboundTimeRange() {
  query.inboundTimeBegin = inboundTimeRange.value[0] || ''
  query.inboundTimeEnd = inboundTimeRange.value[1] || ''
}

function applyRouteQuery() {
  if (typeof route.query.status === 'string') {
    query.status = route.query.status
  }
}

async function loadData() {
  syncInboundTimeRange()
  loading.value = true
  try {
    const result = await getInboundPageApi(query)
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
    inboundNo: '',
    arrivalNo: '',
    orderNo: '',
    warehouseName: '',
    status: '',
    inboundTimeBegin: '',
    inboundTimeEnd: '',
  })
  inboundTimeRange.value = []
  loadData()
}

function resetForm() {
  Object.assign(inboundForm, {
    id: undefined,
    arrivalId: undefined,
    remark: '',
  })
  previewArrival.value = null
  previewArrivalItems.value = []
  selectedArrival.value = null
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
  loadSelectableArrivals()
}

async function loadSelectableArrivals() {
  arrivalLoading.value = true
  try {
    const result = await getArrivalPageApi(arrivalQuery)
    arrivalRows.value = result.records
    arrivalTotal.value = result.total
  } finally {
    arrivalLoading.value = false
  }
}

function openArrivalDialog() {
  arrivalDialogVisible.value = true
  if (!arrivalRows.value.length) {
    loadSelectableArrivals()
  }
}

function handleArrivalSearch() {
  arrivalQuery.pageNum = 1
  loadSelectableArrivals()
}

function handleArrivalReset() {
  Object.assign(arrivalQuery, {
    pageNum: 1,
    pageSize: 10,
    arrivalNo: '',
    orderNo: '',
    warehouseName: '',
    status: '',
    pendingInboundOnly: true,
  })
  loadSelectableArrivals()
}

async function selectArrival(row: ArrivalVO) {
  selectedArrival.value = row
  inboundForm.arrivalId = row.id
  arrivalDialogVisible.value = false
  formRef.value?.validateField('arrivalId')
  await loadArrivalPreview()
}

function handleArrivalSizeChange(size: number) {
  arrivalQuery.pageSize = size
  arrivalQuery.pageNum = 1
  loadSelectableArrivals()
}

function handleArrivalCurrentChange(page: number) {
  arrivalQuery.pageNum = page
  loadSelectableArrivals()
}

async function loadArrivalPreview() {
  if (!inboundForm.arrivalId) {
    ElMessage.warning('请先选择到货单')
    return
  }

  previewLoading.value = true
  try {
    const result = await getArrivalByIdApi(inboundForm.arrivalId)
    previewArrival.value = result
    selectedArrival.value = result
    previewArrivalItems.value = result.items || []
    ElMessage.success('已加载到货详情')
  } finally {
    previewLoading.value = false
  }
}

async function submitForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    await addInboundApi({ ...inboundForm })
    ElMessage.success('入库单已生成，等待仓库确认入库')
    dialogVisible.value = false
    query.status = 'PENDING'
    loadData()
  } finally {
    formLoading.value = false
  }
}

function canCancelRow(row: InboundVO) {
  return canManage.value && row.status === 'PENDING'
}

function canConfirmRow(row: InboundVO) {
  return canManage.value && row.status === 'PENDING'
}

async function handleCancel(row: InboundVO) {
  await ElMessageBox.confirm(`确定取消入库单 ${row.inboundNo || row.id} 吗？`, '取消入库单', {
    type: 'warning',
    confirmButtonText: '取消入库',
    cancelButtonText: '返回',
  })

  await cancelInboundApi({ id: row.id })
  ElMessage.success('入库单已取消')
  loadData()
  if (detail.value?.id === row.id) {
    loadDetail(row.id)
  }
}

async function handleConfirm(row: InboundVO) {
  await ElMessageBox.confirm(`确定确认入库单 ${row.inboundNo || row.id} 吗？`, '确认入库', {
    type: 'warning',
    confirmButtonText: '确认入库',
    cancelButtonText: '返回',
  })

  await confirmInboundApi({ id: row.id })
  ElMessage.success('入库单已确认，库存台账已更新')
  loadData()
  if (detail.value?.id === row.id) {
    loadDetail(row.id)
  }
  try {
    await ElMessageBox.confirm('入库单已确认，库存台账和库存流水已更新。是否前往库存台账查看？', '下一步处理', {
      type: 'success',
      confirmButtonText: '查看库存台账',
      cancelButtonText: '稍后查看',
    })
    router.push('/warehouse/inventory')
  } catch {
    // 用户选择稍后查看时保持当前页面
  }
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    const result = await getInboundByIdApi(id)
    detail.value = result
    detailItems.value = result.items || []
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: InboundVO) {
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

onMounted(async () => {
  applyRouteQuery()
  await loadData()
  if (canManage.value && route.query.todo === 'pendingArrival') {
    openCreateDialog()
  }
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
        生成入库单
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="入库单号">
            <el-input v-model="query.inboundNo" clearable placeholder="输入入库单号" @keyup.enter="handleSearch" />
          </el-form-item>
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
              <el-option v-for="item in inboundStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="入库时间">
            <el-date-picker
              v-model="inboundTimeRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
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
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed" :empty-text="inboundEmptyText">
          <el-table-column prop="inboundNo" label="入库单号" min-width="160" fixed="left" show-overflow-tooltip />
          <el-table-column prop="arrivalNo" label="到货单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="inboundNumber" label="入库数量" min-width="110" />
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(inboundStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(inboundStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="入库时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.inboundTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="canConfirmRow(row)" link type="primary" @click="handleConfirm(row)">确认入库</el-button>
                <el-button v-if="canCancelRow(row)" link type="danger" @click="handleCancel(row)">取消</el-button>
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

    <el-dialog v-model="dialogVisible" title="生成入库单" width="1100px" @closed="resetForm">
      <el-form ref="formRef" v-loading="formLoading" :model="inboundForm" :rules="formRules" label-width="108px">
        <div class="dialog-grid">
          <el-form-item label="到货单" prop="arrivalId">
            <el-input
              :model-value="selectedArrival ? `${selectedArrival.arrivalNo || ''} / ${selectedArrival.orderNo || ''}` : ''"
              readonly
              placeholder="选择待入库到货单"
            >
              <template #append>
                <el-button @click="openArrivalDialog">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" :rows="2" type="textarea" placeholder="填写入库备注" />
        </el-form-item>

        <section class="content-panel form-embed-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">到货预览</div>
              <div class="page-desc">核对本次准备入库的到货单和合格数量</div>
            </div>
            <el-button :loading="previewLoading" @click="loadArrivalPreview">
              <el-icon><Refresh /></el-icon>
              加载到货详情
            </el-button>
          </div>

          <div v-if="previewArrival" class="detail-card preview-card">
            <h3 class="detail-card__title">到货信息</h3>
            <el-descriptions class="detail-descriptions" :column="2" border>
              <el-descriptions-item label="到货单号">{{ formatEmpty(previewArrival.arrivalNo) }}</el-descriptions-item>
              <el-descriptions-item label="订单号">{{ formatEmpty(previewArrival.orderNo) }}</el-descriptions-item>
              <el-descriptions-item label="仓库名称">{{ formatEmpty(previewArrival.warehouseName) }}</el-descriptions-item>
              <el-descriptions-item label="到货日期">{{ formatDate(previewArrival.arrivalDate) }}</el-descriptions-item>
              <el-descriptions-item label="到货数量">{{ formatEmpty(previewArrival.arrivalNumber) }}</el-descriptions-item>
              <el-descriptions-item label="合格数量">{{ formatEmpty(previewArrival.qualifiedNumber) }}</el-descriptions-item>
              <el-descriptions-item label="不合格数量">{{ formatEmpty(previewArrival.unqualifiedNumber) }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag class="status-tag" :type="getOptionType(arrivalStatusOptions, previewArrival.status)" effect="plain">
                  {{ getOptionLabel(arrivalStatusOptions, previewArrival.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="异常说明">{{ formatEmpty(previewArrival.abnormalNote) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="table-wrap">
            <el-table
              v-loading="previewLoading"
              :data="previewArrivalItems"
              table-layout="fixed"
              empty-text="请选择到货单后加载到货明细"
            >
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
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">生成入库单</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="arrivalDialogVisible" title="选择待入库到货单" width="980px">
      <div class="filter-section">
        <el-form :model="arrivalQuery" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="到货单号">
            <el-input
              v-model="arrivalQuery.arrivalNo"
              clearable
              placeholder="输入到货单号"
              @keyup.enter="handleArrivalSearch"
            />
          </el-form-item>
          <el-form-item label="订单号">
            <el-input v-model="arrivalQuery.orderNo" clearable placeholder="输入订单号" @keyup.enter="handleArrivalSearch" />
          </el-form-item>
          <el-form-item label="仓库名称">
            <el-input
              v-model="arrivalQuery.warehouseName"
              clearable
              placeholder="输入仓库名称"
              @keyup.enter="handleArrivalSearch"
            />
          </el-form-item>
          <el-form-item label="到货状态">
            <el-select v-model="arrivalQuery.status" clearable placeholder="全部状态" @change="handleArrivalSearch">
              <el-option v-for="item in arrivalStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button @click="handleArrivalReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-button type="primary" @click="handleArrivalSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="table-wrap">
        <el-table
          v-loading="arrivalLoading"
          :data="arrivalRows"
          row-key="id"
          table-layout="fixed"
          empty-text="暂无可生成入库单的到货记录"
        >
          <el-table-column prop="arrivalNo" label="到货单号" min-width="160" show-overflow-tooltip />
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
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectArrival(row)">选择</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="arrivalQuery.pageNum"
          v-model:page-size="arrivalQuery.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="arrivalTotal"
          @size-change="handleArrivalSizeChange"
          @current-change="handleArrivalCurrentChange"
        />
      </div>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="pageTitle + '详情'" size="1180px">
      <div v-loading="detailLoading" class="detail-section">
        <div class="detail-card">
          <h3 class="detail-card__title">入库信息</h3>
          <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="入库单号">{{ formatEmpty(detail.inboundNo) }}</el-descriptions-item>
            <el-descriptions-item label="到货单号">{{ formatEmpty(detail.arrivalNo) }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ formatEmpty(detail.orderNo) }}</el-descriptions-item>
            <el-descriptions-item label="仓库名称">{{ formatEmpty(detail.warehouseName) }}</el-descriptions-item>
            <el-descriptions-item label="入库数量">{{ formatEmpty(detail.inboundNumber) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag class="status-tag" :type="getOptionType(inboundStatusOptions, detail.status)" effect="plain">
                {{ getOptionLabel(inboundStatusOptions, detail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="入库时间">{{ formatDateTime(detail.inboundTime) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <section class="content-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">入库明细</div>
              <div class="page-desc">查看本次入库的物料和合格入库数量</div>
            </div>
          </div>
          <div class="table-wrap">
            <el-table :data="detailItems" table-layout="fixed" empty-text="暂无入库明细">
              <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="88" />
              <el-table-column prop="inboundNumber" label="入库数量" min-width="110" />
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

.preview-card {
  margin: 18px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
