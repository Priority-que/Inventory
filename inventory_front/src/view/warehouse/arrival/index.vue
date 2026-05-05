<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { addArrivalApi, getArrivalByIdApi, getArrivalPageApi, type ArrivalDTO, type ArrivalItemVO, type ArrivalVO } from '@/api/arrival'
import { getPurchaseOrderItemsByOrderIdApi, type PurchaseOrderItemVO } from '@/api/purchaseOrder'
import { arrivalStatusOptions, getOptionLabel, getOptionType } from '@/constants/business'
import { formatDate, formatDateTime, formatEmpty } from '@/utils/format'

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

const canManage = computed(() => route.path.startsWith('/warehouse'))
const pageTitle = computed(() => String(route.meta.title || '到货管理'))
const pageSubtitle = computed(() =>
  canManage.value ? '维护到货单头与到货明细，保留异常与质检数量字段' : '查看到货执行结果与异常到货跟踪字段',
)

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

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ArrivalVO | null>(null)
const detailItems = ref<ArrivalItemVO[]>([])

const formRules: FormRules<ArrivalDTO> = {
  orderId: [{ required: true, message: '请输入采购订单 ID', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请输入仓库 ID', trigger: 'blur' }],
  arrivalDate: [{ required: true, message: '请选择到货日期', trigger: 'change' }],
}

function syncArrivalDateRange() {
  query.arrivalDateBegin = arrivalDateRange.value[0] || ''
  query.arrivalDateEnd = arrivalDateRange.value[1] || ''
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
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function mapOrderItems(items: PurchaseOrderItemVO[]) {
  arrivalItems.value = items.map((item) => {
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
}

async function loadOrderItems() {
  if (!arrivalForm.orderId) {
    ElMessage.warning('请先输入采购订单 ID')
    return
  }

  orderItemsLoading.value = true
  try {
    const items = await getPurchaseOrderItemsByOrderIdApi(arrivalForm.orderId)
    mapOrderItems(items)
    ElMessage.success(`已加载 ${items.length} 条订单明细`)
  } finally {
    orderItemsLoading.value = false
  }
}

async function submitForm() {
  await formRef.value?.validate()
  if (!arrivalItems.value.length) {
    ElMessage.warning('请先加载并维护到货明细')
    return
  }

  formLoading.value = true
  try {
    await addArrivalApi({
      ...arrivalForm,
      items: arrivalItems.value.map((item) => ({
        orderItemId: item.orderItemId,
        arrivalNumber: item.arrivalNumber,
        qualifiedNumber: item.qualifiedNumber,
        unqualifiedNumber: item.unqualifiedNumber,
        abnormalNote: item.abnormalNote,
        remark: item.remark,
      })),
    })
    ElMessage.success('到货单已新增')
    dialogVisible.value = false
    loadData()
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
        新增到货
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
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed">
          <el-table-column prop="id" label="ID" width="88" fixed="left" />
          <el-table-column prop="arrivalNo" label="到货单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="orderId" label="订单 ID" width="100" />
          <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="warehouseId" label="仓库 ID" width="100" />
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
          <el-table-column prop="operatorId" label="操作人 ID" width="110" />
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

    <el-dialog v-model="dialogVisible" title="新增到货" width="1160px" @closed="resetForm">
      <el-form ref="formRef" v-loading="formLoading" :model="arrivalForm" :rules="formRules" label-width="108px">
        <div class="dialog-grid">
          <el-form-item label="订单 ID" prop="orderId">
            <el-input-number v-model="arrivalForm.orderId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="仓库 ID" prop="warehouseId">
            <el-input-number v-model="arrivalForm.warehouseId" :min="1" controls-position="right" />
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
        <el-form-item label="备注">
          <el-input v-model="arrivalForm.remark" :rows="2" type="textarea" placeholder="填写到货备注" />
        </el-form-item>

        <section class="content-panel form-embed-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">到货明细字段</div>
              <div class="page-desc">保留 `orderItemId`、到货数量、合格/不合格数量、异常说明和备注</div>
            </div>
            <el-button :loading="orderItemsLoading" @click="loadOrderItems">
              <el-icon><Refresh /></el-icon>
              加载订单明细
            </el-button>
          </div>
          <div class="table-wrap">
            <el-table v-loading="orderItemsLoading" :data="arrivalItems" table-layout="fixed">
              <el-table-column prop="orderItemId" label="订单明细 ID" width="120" />
              <el-table-column prop="materialId" label="物料 ID" width="100" />
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
              <el-table-column label="到货数量" min-width="132">
                <template #default="{ row }">
                  <el-input-number v-model="row.arrivalNumber" :min="0" controls-position="right" />
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
          <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="pageTitle + '详情'" size="1180px">
      <div v-loading="detailLoading" class="detail-section">
        <div class="detail-card">
          <h3 class="detail-card__title">到货单字段</h3>
          <el-descriptions v-if="detail" class="detail-descriptions" :column="2" border>
            <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="到货单号">{{ formatEmpty(detail.arrivalNo) }}</el-descriptions-item>
            <el-descriptions-item label="订单 ID">{{ formatEmpty(detail.orderId) }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ formatEmpty(detail.orderNo) }}</el-descriptions-item>
            <el-descriptions-item label="仓库 ID">{{ formatEmpty(detail.warehouseId) }}</el-descriptions-item>
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
            <el-descriptions-item label="操作人 ID">{{ formatEmpty(detail.operatorId) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="删除标记">{{ formatEmpty(detail.deleted) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <section class="content-panel">
          <div class="panel-toolbar">
            <div>
              <div class="page-title" style="font-size: 16px">到货明细字段</div>
              <div class="page-desc">保留物料、数量、异常说明、排序号和系统时间字段</div>
            </div>
          </div>
          <div class="table-wrap">
            <el-table :data="detailItems" table-layout="fixed">
              <el-table-column prop="id" label="ID" width="88" />
              <el-table-column prop="arrivalId" label="到货 ID" width="100" />
              <el-table-column prop="orderItemId" label="订单明细 ID" width="120" />
              <el-table-column prop="materialId" label="物料 ID" width="100" />
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
