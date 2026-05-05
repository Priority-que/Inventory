<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  approvePurchaseRequestApi,
  getPurchaseRequestByIdApi,
  getPurchaseRequestItemsByRequestIdApi,
  getPurchaseRequestPageApi,
  getPurchaseRequestReviewsByRequestIdApi,
  rejectPurchaseRequestApi,
  type PurchaseRequestDTO,
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
  status: 'PENDING_APPROVAL',
})
const submitTimeRange = ref<string[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<PurchaseRequestVO | null>(null)
const detailItems = ref<PurchaseRequestItemVO[]>([])
const reviewList = ref<PurchaseRequestReviewVO[]>([])
const detailTab = ref('base')

const actionDialogVisible = ref(false)
const actionMode = ref<'approve' | 'reject'>('approve')
const actionLoading = ref(false)
const actionFormRef = ref<FormInstance>()
const actionForm = reactive<PurchaseRequestDTO>({
  id: undefined,
  requestNo: '',
  title: '',
  reviewNote: '',
})

const actionTitle = computed(() => (actionMode.value === 'approve' ? '审批通过采购申请' : '驳回采购申请'))
const actionButtonText = computed(() => (actionMode.value === 'approve' ? '审批通过' : '确认驳回'))
const actionTagType = computed(() => (actionMode.value === 'approve' ? 'success' : 'danger'))
const canReviewDetail = computed(() => detail.value?.status === 'PENDING_APPROVAL')

const actionRules: FormRules<PurchaseRequestDTO> = {
  reviewNote: [{ required: true, message: '请输入审批意见', trigger: 'blur' }],
}

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
    status: 'PENDING_APPROVAL',
  })
  submitTimeRange.value = []
  loadData()
}

function canApproveRow(row: PurchaseRequestPageVO) {
  return row.status === 'PENDING_APPROVAL'
}

function resetActionForm() {
  Object.assign(actionForm, {
    id: undefined,
    requestNo: '',
    title: '',
    reviewNote: '',
  })
  actionFormRef.value?.clearValidate()
}

function openActionDialog(row: Pick<PurchaseRequestPageVO, 'id' | 'requestNo' | 'title'>, mode: 'approve' | 'reject') {
  actionMode.value = mode
  resetActionForm()
  Object.assign(actionForm, {
    id: row.id,
    requestNo: row.requestNo,
    title: row.title,
  })
  actionDialogVisible.value = true
}

async function submitAction() {
  await actionFormRef.value?.validate()
  actionLoading.value = true
  try {
    if (actionMode.value === 'approve') {
      await approvePurchaseRequestApi({
        id: actionForm.id,
        reviewNote: actionForm.reviewNote,
      })
      ElMessage.success('采购申请已审批通过')
    } else {
      await rejectPurchaseRequestApi({
        id: actionForm.id,
        reviewNote: actionForm.reviewNote,
      })
      ElMessage.success('采购申请已驳回')
    }

    actionDialogVisible.value = false
    await loadData()

    if (detail.value?.id === actionForm.id) {
      await loadDetail(actionForm.id as number)
    }
  } finally {
    actionLoading.value = false
  }
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
        <h2 class="page-title">采购审批</h2>
        <p class="page-subtitle">查看采购申请、申请明细与审批历史，并对待审批单据执行通过或驳回</p>
      </div>
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

      <div class="table-wrap">
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed">
          <el-table-column prop="id" label="ID" width="88" fixed="left" />
          <el-table-column prop="requestNo" label="申请单号" min-width="168" show-overflow-tooltip />
          <el-table-column prop="title" label="标题" min-width="188" show-overflow-tooltip />
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
          <el-table-column label="审批意见" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.reviewNote) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="116">
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
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button
                  v-if="canApproveRow(row)"
                  link
                  type="primary"
                  @click="openActionDialog(row, 'approve')"
                >
                  通过
                </el-button>
                <el-button
                  v-if="canApproveRow(row)"
                  link
                  type="danger"
                  @click="openActionDialog(row, 'reject')"
                >
                  驳回
                </el-button>
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

    <el-drawer v-model="detailVisible" title="采购申请详情" size="1120px">
      <div v-loading="detailLoading" class="detail-section">
        <div v-if="canReviewDetail && detail" class="detail-card action-card">
          <div class="action-card__content">
            <div>
              <h3 class="detail-card__title">审批动作</h3>
              <p class="action-card__desc">当前单据仍处于待审批状态，可在详情中直接完成通过或驳回。</p>
            </div>
            <div class="action-card__buttons">
              <el-button type="primary" @click="openActionDialog(detail, 'approve')">审批通过</el-button>
              <el-button type="danger" plain @click="openActionDialog(detail, 'reject')">驳回申请</el-button>
            </div>
          </div>
        </div>

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
                <el-descriptions-item label="审批意见">{{ formatEmpty(detail.reviewNote) }}</el-descriptions-item>
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
                  <div class="page-title" style="font-size: 16px">申请明细字段</div>
                  <div class="page-desc">完整展示采购申请明细，不开放采购经理修改。</div>
                </div>
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
                </el-table>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="审批记录" name="reviews">
            <section class="content-panel">
              <div class="panel-toolbar">
                <div>
                  <div class="page-title" style="font-size: 16px">审批历史字段</div>
                  <div class="page-desc">按接口返回顺序展示审批动作、状态流转和审批意见。</div>
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
                  <el-table-column prop="operateNote" label="审批意见" min-width="180" show-overflow-tooltip />
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

    <el-dialog v-model="actionDialogVisible" :title="actionTitle" width="640px" @closed="resetActionForm">
      <el-form
        ref="actionFormRef"
        v-loading="actionLoading"
        :model="actionForm"
        :rules="actionRules"
        label-width="100px"
      >
        <el-form-item label="申请单号">
          <el-input v-model="actionForm.requestNo" disabled />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="actionForm.title" disabled />
        </el-form-item>
        <el-form-item label="审批动作">
          <el-tag :type="actionTagType" effect="plain">{{ actionMode === 'approve' ? '审批通过' : '审批驳回' }}</el-tag>
        </el-form-item>
        <el-form-item label="审批意见" prop="reviewNote">
          <el-input
            v-model="actionForm.reviewNote"
            :rows="4"
            type="textarea"
            :placeholder="actionMode === 'approve' ? '请输入审批通过意见' : '请输入驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="actionDialogVisible = false">取消</el-button>
          <el-button :type="actionMode === 'approve' ? 'primary' : 'danger'" :loading="actionLoading" @click="submitAction">
            {{ actionButtonText }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.action-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.action-card__desc {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.action-card__buttons {
  display: flex;
  gap: 12px;
}

@media (max-width: 720px) {
  .action-card__content,
  .action-card__buttons {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
