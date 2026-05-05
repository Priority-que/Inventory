<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  addPurchaseRequestApi,
  addPurchaseRequestItemApi,
  deletePurchaseRequestApi,
  deletePurchaseRequestItemApi,
  getPurchaseRequestByIdApi,
  getPurchaseRequestItemsByRequestIdApi,
  getPurchaseRequestPageApi,
  getPurchaseRequestReviewsByRequestIdApi,
  submitPurchaseRequestApi,
  updatePurchaseRequestApi,
  updatePurchaseRequestItemApi,
  withdrawPurchaseRequestApi,
  type PurchaseRequestDTO,
  type PurchaseRequestItemDTO,
  type PurchaseRequestItemVO,
  type PurchaseRequestPageVO,
  type PurchaseRequestReviewVO,
  type PurchaseRequestVO,
} from '@/api/purchaseRequest'
import {
  getOptionLabel,
  getOptionType,
  purchaseRequestStatusOptions,
  purchaseReviewActionOptions,
} from '@/constants/business'
import { formatDate, formatDateTime, formatEmpty } from '@/utils/format'

const authStore = useAuthStore()

const loading = ref(false)
const tableData = ref<PurchaseRequestPageVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  requestNo: '',
  title: '',
  dept: '',
  submitTimeBegin: '',
  submitTimeEnd: '',
  status: '',
})
const submitTimeRange = ref<string[]>([])

const selectedRows = ref<PurchaseRequestPageVO[]>([])
const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const requestForm = reactive<PurchaseRequestDTO>({
  applicantId: authStore.user?.id,
  title: '',
  dept: '',
  expectedDate: '',
  reviewNote: '',
  status: 'DRAFT',
  remark: '',
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<PurchaseRequestVO | null>(null)
const detailItems = ref<PurchaseRequestItemVO[]>([])
const reviewList = ref<PurchaseRequestReviewVO[]>([])
const detailTab = ref('base')

const itemDialogVisible = ref(false)
const itemDialogMode = ref<'create' | 'edit'>('create')
const itemFormRef = ref<FormInstance>()
const itemFormLoading = ref(false)
const itemForm = reactive<PurchaseRequestItemDTO>({
  requestId: undefined,
  materialId: undefined,
  materialCode: '',
  materialName: '',
  specification: '',
  unit: '',
  requestNumber: undefined,
  sortNumber: undefined,
  remark: '',
})

const formRules: FormRules<PurchaseRequestDTO> = {
  title: [{ required: true, message: '请输入申请标题', trigger: 'blur' }],
  dept: [{ required: true, message: '请输入申请部门', trigger: 'blur' }],
  expectedDate: [{ required: true, message: '请选择期望到货日期', trigger: 'change' }],
}

const itemFormRules: FormRules<PurchaseRequestItemDTO> = {
  materialId: [{ required: true, message: '请输入物料 ID', trigger: 'blur' }],
  materialCode: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  specification: [{ required: true, message: '请输入规格型号', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  requestNumber: [{ required: true, message: '请输入申请数量', trigger: 'blur' }],
}

const canManageItems = computed(() => {
  const status = detail.value?.status || ''
  return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status)
})

function syncSubmitRange() {
  query.submitTimeBegin = submitTimeRange.value[0] || ''
  query.submitTimeEnd = submitTimeRange.value[1] || ''
}

async function loadData() {
  syncSubmitRange()
  loading.value = true
  try {
    const result = await getPurchaseRequestPageApi(query)
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
    requestNo: '',
    title: '',
    dept: '',
    submitTimeBegin: '',
    submitTimeEnd: '',
    status: '',
  })
  submitTimeRange.value = []
  loadData()
}

function handleSelectionChange(rows: PurchaseRequestPageVO[]) {
  selectedRows.value = rows
}

function resetForm() {
  Object.assign(requestForm, {
    id: undefined,
    requestNo: '',
    applicantId: authStore.user?.id,
    title: '',
    dept: '',
    expectedDate: '',
    submitTime: '',
    reviewUserId: undefined,
    reviewTime: '',
    reviewNote: '',
    status: 'DRAFT',
    remark: '',
    createTime: '',
    updateTime: '',
    deleted: undefined,
  })
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: PurchaseRequestPageVO) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(requestForm, {
    id: row.id,
    requestNo: row.requestNo,
    applicantId: row.applicantId,
    title: row.title,
    dept: row.dept,
    expectedDate: row.expectedDate,
    submitTime: row.submitTime,
    reviewUserId: row.reviewUserId,
    reviewTime: row.reviewTime,
    reviewNote: row.reviewNote,
    status: row.status,
    remark: row.remark,
    createTime: row.createTime,
    updateTime: row.updateTime,
    deleted: row.deleted,
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await addPurchaseRequestApi({ ...requestForm })
      ElMessage.success('采购申请已新增')
    } else {
      await updatePurchaseRequestApi({ ...requestForm })
      ElMessage.success('采购申请已更新')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    formLoading.value = false
  }
}

function canEditRow(row: PurchaseRequestPageVO) {
  return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(row.status || '')
}

function canSubmitRow(row: PurchaseRequestPageVO) {
  return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(row.status || '')
}

function canWithdrawRow(row: PurchaseRequestPageVO) {
  return row.status === 'PENDING_APPROVAL'
}

function canDeleteRow(row: PurchaseRequestPageVO) {
  return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(row.status || '')
}

async function handleSubmit(row: PurchaseRequestPageVO) {
  await ElMessageBox.confirm(`确定提交采购申请 ${row.requestNo || row.id} 吗？`, '提交采购申请', {
    type: 'warning',
    confirmButtonText: '提交',
    cancelButtonText: '取消',
  })

  await submitPurchaseRequestApi({ id: row.id })
  ElMessage.success('采购申请已提交')
  loadData()
}

async function handleWithdraw(row: PurchaseRequestPageVO) {
  await ElMessageBox.confirm(`确定撤回采购申请 ${row.requestNo || row.id} 吗？`, '撤回采购申请', {
    type: 'warning',
    confirmButtonText: '撤回',
    cancelButtonText: '取消',
  })

  await withdrawPurchaseRequestApi({ id: row.id })
  ElMessage.success('采购申请已撤回')
  loadData()
  if (detail.value?.id === row.id) {
    loadDetail(row.id)
  }
}

async function handleDelete(ids: number[], confirmed = false) {
  if (!ids.length) {
    ElMessage.warning('请选择要删除的采购申请')
    return
  }

  if (!confirmed) {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条采购申请吗？`, '删除采购申请', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  }

  await deletePurchaseRequestApi(ids)
  ElMessage.success('采购申请已删除')
  loadData()
}

async function loadDetail(id: number) {
  detailLoading.value = true
  try {
    const [base, items, reviews] = await Promise.all([
      getPurchaseRequestByIdApi(id),
      getPurchaseRequestItemsByRequestIdApi(id),
      getPurchaseRequestReviewsByRequestIdApi(id),
    ])
    detail.value = base
    detailItems.value = items
    reviewList.value = reviews
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row: PurchaseRequestPageVO) {
  detailTab.value = 'base'
  detailVisible.value = true
  await loadDetail(row.id)
}

function resetItemForm() {
  Object.assign(itemForm, {
    id: undefined,
    requestId: detail.value?.id,
    materialId: undefined,
    materialCode: '',
    materialName: '',
    specification: '',
    unit: '',
    requestNumber: undefined,
    sortNumber: undefined,
    remark: '',
    createTime: '',
    updateTime: '',
    deleted: undefined,
  })
  itemFormRef.value?.clearValidate()
}

function openCreateItemDialog() {
  itemDialogMode.value = 'create'
  resetItemForm()
  itemDialogVisible.value = true
}

function openEditItemDialog(row: PurchaseRequestItemVO) {
  itemDialogMode.value = 'edit'
  resetItemForm()
  Object.assign(itemForm, {
    id: row.id,
    requestId: row.requestId,
    materialId: row.materialId,
    materialCode: row.materialCode,
    materialName: row.materialName,
    specification: row.specification,
    unit: row.unit,
    requestNumber: row.requestNumber,
    sortNumber: row.sortNumber,
    remark: row.remark,
    createTime: row.createTime,
    updateTime: row.updateTime,
    deleted: row.deleted,
  })
  itemDialogVisible.value = true
}

async function submitItemForm() {
  if (!detail.value?.id) {
    return
  }

  await itemFormRef.value?.validate()
  itemFormLoading.value = true
  try {
    if (itemDialogMode.value === 'create') {
      await addPurchaseRequestItemApi({
        ...itemForm,
        requestId: detail.value.id,
      })
      ElMessage.success('申请明细已新增')
    } else {
      await updatePurchaseRequestItemApi({ ...itemForm })
      ElMessage.success('申请明细已更新')
    }
    itemDialogVisible.value = false
    await loadDetail(detail.value.id)
  } finally {
    itemFormLoading.value = false
  }
}

async function handleDeleteItem(row: PurchaseRequestItemVO) {
  await deletePurchaseRequestItemApi([row.id])
  ElMessage.success('申请明细已删除')
  if (detail.value?.id) {
    loadDetail(detail.value.id)
  }
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
        <h2 class="page-title">采购申请</h2>
        <p class="page-subtitle">维护采购申请单头、申请明细和审批流转信息</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增采购申请
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="申请单号">
            <el-input v-model="query.requestNo" clearable placeholder="输入申请单号" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="query.title" clearable placeholder="输入申请标题" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="申请部门">
            <el-input v-model="query.dept" clearable placeholder="输入申请部门" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in purchaseRequestStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="提交时间">
            <el-date-picker
              v-model="submitTimeRange"
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

      <div class="batch-action-bar">
        <span>已选 <strong class="batch-count">{{ selectedIds.length }}</strong> 项</span>
        <el-button type="danger" plain :disabled="!selectedIds.length" @click="handleDelete(selectedIds)">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="tableData"
          row-key="id"
          table-layout="fixed"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="46" fixed="left" />
          <el-table-column prop="id" label="ID" width="88" fixed="left" />
          <el-table-column prop="requestNo" label="申请单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="applicantId" label="申请人 ID" width="110" />
          <el-table-column prop="applicantName" label="申请人" min-width="120" show-overflow-tooltip />
          <el-table-column prop="dept" label="申请部门" min-width="120" show-overflow-tooltip />
          <el-table-column label="期望到货日期" min-width="128">
            <template #default="{ row }">{{ formatDate(row.expectedDate) }}</template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column prop="reviewUserId" label="审批人 ID" width="110" />
          <el-table-column prop="reviewUserName" label="审批人" min-width="120" show-overflow-tooltip />
          <el-table-column label="审批时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.reviewTime) }}</template>
          </el-table-column>
          <el-table-column label="审批备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.reviewNote) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(purchaseRequestStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(purchaseRequestStatusOptions, row.status) }}
              </el-tag>
            </template>
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
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="canEditRow(row)" link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button v-if="canSubmitRow(row)" link type="primary" @click="handleSubmit(row)">提交</el-button>
                <el-button v-if="canWithdrawRow(row)" link type="warning" @click="handleWithdraw(row)">撤回</el-button>
                <el-popconfirm
                  v-if="canDeleteRow(row)"
                  title="确定删除该采购申请吗？"
                  confirm-button-text="删除"
                  @confirm="handleDelete([row.id], true)"
                >
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
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
      :title="dialogMode === 'create' ? '新增采购申请' : '编辑采购申请'"
      width="820px"
      @closed="resetForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="requestForm" :rules="formRules" label-width="108px">
        <div class="dialog-grid">
          <el-form-item label="申请标题" prop="title">
            <el-input v-model="requestForm.title" placeholder="输入采购申请标题" />
          </el-form-item>
          <el-form-item label="申请部门" prop="dept">
            <el-input v-model="requestForm.dept" placeholder="输入申请部门" />
          </el-form-item>
          <el-form-item label="期望到货日期" prop="expectedDate">
            <el-date-picker
              v-model="requestForm.expectedDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="requestForm.status" placeholder="选择状态" disabled>
              <el-option
                v-for="item in purchaseRequestStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="requestForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>

        <el-collapse>
          <el-collapse-item title="系统字段与只读字段" name="system">
            <el-descriptions class="detail-descriptions" :column="2" border>
              <el-descriptions-item label="ID">{{ formatEmpty(requestForm.id) }}</el-descriptions-item>
              <el-descriptions-item label="申请单号">{{ formatEmpty(requestForm.requestNo) }}</el-descriptions-item>
              <el-descriptions-item label="申请人 ID">{{ formatEmpty(requestForm.applicantId) }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ formatDateTime(requestForm.submitTime) }}</el-descriptions-item>
              <el-descriptions-item label="审批人 ID">{{ formatEmpty(requestForm.reviewUserId) }}</el-descriptions-item>
              <el-descriptions-item label="审批时间">{{ formatDateTime(requestForm.reviewTime) }}</el-descriptions-item>
              <el-descriptions-item label="审批备注">{{ formatEmpty(requestForm.reviewNote) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(requestForm.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDateTime(requestForm.updateTime) }}</el-descriptions-item>
              <el-descriptions-item label="删除标记">{{ formatEmpty(requestForm.deleted) }}</el-descriptions-item>
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

    <el-drawer v-model="detailVisible" title="采购申请详情" size="1120px">
      <div v-loading="detailLoading" class="detail-section">
        <el-tabs v-model="detailTab">
          <el-tab-pane label="基本信息" name="base">
            <div class="detail-card">
              <h3 class="detail-card__title">申请单字段</h3>
              <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
                <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
                <el-descriptions-item label="申请单号">{{ formatEmpty(detail.requestNo) }}</el-descriptions-item>
                <el-descriptions-item label="标题">{{ formatEmpty(detail.title) }}</el-descriptions-item>
                <el-descriptions-item label="申请人 ID">{{ formatEmpty(detail.applicantId) }}</el-descriptions-item>
                <el-descriptions-item label="申请部门">{{ formatEmpty(detail.dept) }}</el-descriptions-item>
                <el-descriptions-item label="期望到货日期">{{ formatDate(detail.expectedDate) }}</el-descriptions-item>
                <el-descriptions-item label="提交时间">{{ formatDateTime(detail.submitTime) }}</el-descriptions-item>
                <el-descriptions-item label="审批人 ID">{{ formatEmpty(detail.reviewUserId) }}</el-descriptions-item>
                <el-descriptions-item label="审批时间">{{ formatDateTime(detail.reviewTime) }}</el-descriptions-item>
                <el-descriptions-item label="审批备注">{{ formatEmpty(detail.reviewNote) }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag
                    class="status-tag"
                    :type="getOptionType(purchaseRequestStatusOptions, detail.status)"
                    effect="plain"
                  >
                    {{ getOptionLabel(purchaseRequestStatusOptions, detail.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
                <el-descriptions-item label="删除标记">{{ formatEmpty(detail.deleted) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <el-tab-pane label="申请明细" name="items">
            <section class="content-panel">
              <div class="panel-toolbar">
                <div>
                  <div class="page-title" style="font-size: 16px">明细字段</div>
                  <div class="page-desc">按采购申请维护物料、数量、排序和备注</div>
                </div>
                <el-button v-if="canManageItems" type="primary" @click="openCreateItemDialog">
                  <el-icon><Plus /></el-icon>
                  新增明细
                </el-button>
              </div>
              <div class="table-wrap">
                <el-table :data="detailItems" table-layout="fixed">
                  <el-table-column prop="id" label="ID" width="88" />
                  <el-table-column prop="requestId" label="申请 ID" width="100" />
                  <el-table-column prop="requestNo" label="申请单号" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="requestTitle" label="申请标题" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="materialId" label="物料 ID" width="100" />
                  <el-table-column prop="materialCode" label="物料编码" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="materialName" label="物料名称" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="specification" label="规格型号" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="unit" label="单位" width="88" />
                  <el-table-column prop="requestNumber" label="申请数量" min-width="120" />
                  <el-table-column prop="sortNumber" label="排序号" width="88" />
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
                  <el-table-column v-if="canManageItems" label="操作" width="160" fixed="right">
                    <template #default="{ row }">
                      <div class="table-actions">
                        <el-button link type="primary" @click="openEditItemDialog(row)">编辑</el-button>
                        <el-popconfirm
                          title="确定删除该申请明细吗？"
                          confirm-button-text="删除"
                          @confirm="handleDeleteItem(row)"
                        >
                          <template #reference>
                            <el-button link type="danger">删除</el-button>
                          </template>
                        </el-popconfirm>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="审批记录" name="reviews">
            <section class="content-panel">
              <div class="panel-toolbar">
                <div>
                  <div class="page-title" style="font-size: 16px">审批字段</div>
                  <div class="page-desc">保留 actionType、状态流转、操作人与说明</div>
                </div>
              </div>
              <div class="table-wrap">
                <el-table :data="reviewList" table-layout="fixed">
                  <el-table-column prop="id" label="ID" width="88" />
                  <el-table-column prop="requestId" label="申请 ID" width="100" />
                  <el-table-column label="动作类型" min-width="120">
                    <template #default="{ row }">
                      <el-tag
                        class="status-tag"
                        :type="getOptionType(purchaseReviewActionOptions, row.actionType)"
                        effect="plain"
                      >
                        {{ getOptionLabel(purchaseReviewActionOptions, row.actionType) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="fromStatus" label="原状态" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="toStatus" label="目标状态" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="operatorId" label="操作人 ID" width="110" />
                  <el-table-column prop="operatorName" label="操作人" min-width="120" show-overflow-tooltip />
                  <el-table-column prop="operateNote" label="操作说明" min-width="180" show-overflow-tooltip />
                  <el-table-column label="操作时间" min-width="168">
                    <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
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
      :title="itemDialogMode === 'create' ? '新增申请明细' : '编辑申请明细'"
      width="760px"
      @closed="resetItemForm"
    >
      <el-form ref="itemFormRef" v-loading="itemFormLoading" :model="itemForm" :rules="itemFormRules" label-width="100px">
        <div class="dialog-grid">
          <el-form-item label="申请 ID">
            <el-input :model-value="String(itemForm.requestId || '')" disabled />
          </el-form-item>
          <el-form-item label="物料 ID" prop="materialId">
            <el-input-number v-model="itemForm.materialId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="物料编码" prop="materialCode">
            <el-input v-model="itemForm.materialCode" placeholder="输入物料编码" />
          </el-form-item>
          <el-form-item label="物料名称" prop="materialName">
            <el-input v-model="itemForm.materialName" placeholder="输入物料名称" />
          </el-form-item>
          <el-form-item label="规格型号" prop="specification">
            <el-input v-model="itemForm.specification" placeholder="输入规格型号" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model="itemForm.unit" placeholder="输入单位" />
          </el-form-item>
          <el-form-item label="申请数量" prop="requestNumber">
            <el-input-number v-model="itemForm.requestNumber" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="排序号">
            <el-input-number v-model="itemForm.sortNumber" :min="0" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>

        <el-collapse>
          <el-collapse-item title="明细系统字段" name="item-system">
            <el-descriptions class="detail-descriptions" :column="2" border>
              <el-descriptions-item label="ID">{{ formatEmpty(itemForm.id) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(itemForm.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDateTime(itemForm.updateTime) }}</el-descriptions-item>
              <el-descriptions-item label="删除标记">{{ formatEmpty(itemForm.deleted) }}</el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
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
:deep(.el-input-number) {
  width: 100%;
}
</style>
