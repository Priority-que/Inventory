<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  adjustInventoryApi,
  getInventoryLogPageApi,
  getInventoryPageApi,
  type InventoryLogPageVO,
  type InventoryPageVO,
} from '@/api/inventory'
import { formatDateTime, formatEmpty } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const tableData = ref<InventoryPageVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentInventory = ref<InventoryPageVO | null>(null)
const detailActiveTab = ref('base')
const logLoading = ref(false)
const logRows = ref<InventoryLogPageVO[]>([])
const logTotal = ref(0)
const adjustDialogVisible = ref(false)
const adjustLoading = ref(false)
const adjustFormRef = ref<FormInstance>()
const canAdjust = computed(() => {
  const roles = authStore.user?.roleCodes || []
  return roles.includes('ADMIN') || roles.includes('WAREHOUSE')
})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialCode: '',
  materialName: '',
  warehouseName: '',
  stockStatus: '',
})

const inventoryEmptyText = computed(() => {
  if (query.stockStatus === 'LOW') {
    return '暂无低库存预警'
  }
  if (query.stockStatus === 'OVER') {
    return '暂无超储预警'
  }
  if (query.stockStatus === 'NORMAL') {
    return '暂无正常库存记录'
  }
  return '暂无库存台账'
})

const logQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  inventoryId: undefined as number | undefined,
})

const adjustForm = reactive({
  inventoryId: undefined as number | undefined,
  changeNumber: undefined as number | undefined,
  reason: '',
})

const adjustRules: FormRules = {
  changeNumber: [{ required: true, message: '请输入调整数量', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入调整原因', trigger: 'blur' }],
}

const stockStatusOptions = [
  { label: '正常', value: 'NORMAL', type: 'success' },
  { label: '低库存', value: 'LOW', type: 'warning' },
  { label: '超储', value: 'OVER', type: 'danger' },
] as const

const bizTypeOptions = [
  { label: '入库', value: 'INBOUND', type: 'success' },
  { label: '库存调整', value: 'ADJUST', type: 'warning' },
] as const

function getStockStatusLabel(value?: string) {
  return stockStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

function getStockStatusType(value?: string) {
  return stockStatusOptions.find((item) => item.value === value)?.type || 'info'
}

function getBizTypeLabel(value?: string) {
  return bizTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

function getBizTypeTagType(value?: string) {
  return bizTypeOptions.find((item) => item.value === value)?.type || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const result = await getInventoryPageApi(query)
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
    materialCode: '',
    materialName: '',
    warehouseName: '',
    stockStatus: '',
  })
  loadData()
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

function handleViewDetail(row: InventoryPageVO) {
  currentInventory.value = row
  detailActiveTab.value = 'base'
  Object.assign(logQuery, {
    pageNum: 1,
    pageSize: 10,
    inventoryId: row.id,
  })
  loadLogs()
  detailVisible.value = true
}

async function loadLogs() {
  if (!logQuery.inventoryId) {
    return
  }
  logLoading.value = true
  try {
    const result = await getInventoryLogPageApi(logQuery)
    logRows.value = result.records
    logTotal.value = result.total
  } finally {
    logLoading.value = false
  }
}

function applyRouteQuery() {
  if (typeof route.query.stockStatus === 'string') {
    query.stockStatus = route.query.stockStatus
  }
}

function handleLogSizeChange(size: number) {
  logQuery.pageSize = size
  logQuery.pageNum = 1
  loadLogs()
}

function handleLogCurrentChange(page: number) {
  logQuery.pageNum = page
  loadLogs()
}

function openAdjustDialog(row: InventoryPageVO) {
  Object.assign(adjustForm, {
    inventoryId: row.id,
    changeNumber: undefined,
    reason: '',
  })
  currentInventory.value = row
  adjustDialogVisible.value = true
  adjustFormRef.value?.clearValidate()
}

async function submitAdjust() {
  await adjustFormRef.value?.validate()
  if (!adjustForm.inventoryId) {
    ElMessage.warning('未获取到库存台账信息')
    return
  }
  if (!adjustForm.changeNumber) {
    ElMessage.warning('调整数量不能为0')
    return
  }

  adjustLoading.value = true
  try {
    await adjustInventoryApi({ ...adjustForm })
    ElMessage.success('库存调整已完成，库存台账和库存流水已更新')
    adjustDialogVisible.value = false
    await loadData()
    if (detailVisible.value && logQuery.inventoryId === adjustForm.inventoryId) {
      loadLogs()
    }
  } finally {
    adjustLoading.value = false
  }
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
        <h2 class="page-title">库存台账</h2>
        <p class="page-subtitle">查看物料在各仓库的当前库存、安全库存和预警状态</p>
      </div>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="物料编码">
            <el-input v-model="query.materialCode" clearable placeholder="输入物料编码" @keyup.enter="handleSearch" />
          </el-form-item>

          <el-form-item label="物料名称">
            <el-input v-model="query.materialName" clearable placeholder="输入物料名称" @keyup.enter="handleSearch" />
          </el-form-item>

          <el-form-item label="仓库名称">
            <el-input v-model="query.warehouseName" clearable placeholder="输入仓库名称" @keyup.enter="handleSearch" />
          </el-form-item>

          <el-form-item label="库存状态">
            <el-select v-model="query.stockStatus" clearable placeholder="全部状态">
              <el-option
                v-for="item in stockStatusOptions"
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
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed" :empty-text="inventoryEmptyText">
          <el-table-column prop="materialCode" label="物料编码" min-width="130" show-overflow-tooltip />
          <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="warehouseName" label="仓库名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="currentNumber" label="当前库存" min-width="110" />
          <el-table-column prop="safetyNumber" label="安全库存" min-width="110" />
          <el-table-column prop="upperNumber" label="库存上限" min-width="110" />

          <el-table-column label="库存状态" width="110">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getStockStatusType(row.stockStatus)" effect="plain">
                {{ getStockStatusLabel(row.stockStatus) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="最后入库时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.lastInboundTime) }}</template>
          </el-table-column>

          <el-table-column label="备注" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleViewDetail(row)">查看</el-button>
              <el-button v-if="canAdjust" type="warning" link @click="openAdjustDialog(row)">调整</el-button>
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

    <el-drawer v-model="detailVisible" title="库存台账详情" size="920px">
      <el-tabs v-model="detailActiveTab">
        <el-tab-pane label="基础信息" name="base">
          <el-descriptions v-if="currentInventory" class="detail-descriptions" :column="1" border>
            <el-descriptions-item label="物料编码">
              {{ formatEmpty(currentInventory.materialCode) }}
            </el-descriptions-item>
            <el-descriptions-item label="物料名称">
              {{ formatEmpty(currentInventory.materialName) }}
            </el-descriptions-item>
            <el-descriptions-item label="规格型号">
              {{ formatEmpty(currentInventory.specification) }}
            </el-descriptions-item>
            <el-descriptions-item label="单位">
              {{ formatEmpty(currentInventory.unit) }}
            </el-descriptions-item>
            <el-descriptions-item label="仓库名称">
              {{ formatEmpty(currentInventory.warehouseName) }}
            </el-descriptions-item>
            <el-descriptions-item label="库存状态">
              <el-tag :type="getStockStatusType(currentInventory.stockStatus)" effect="plain">
                {{ getStockStatusLabel(currentInventory.stockStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前库存">
              {{ formatEmpty(currentInventory.currentNumber) }}
            </el-descriptions-item>
            <el-descriptions-item label="安全库存">
              {{ formatEmpty(currentInventory.safetyNumber) }}
            </el-descriptions-item>
            <el-descriptions-item label="库存上限">
              {{ formatEmpty(currentInventory.upperNumber) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后入库时间">
              {{ formatDateTime(currentInventory.lastInboundTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(currentInventory.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDateTime(currentInventory.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">
              {{ formatEmpty(currentInventory.remark) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="库存流水" name="logs">
          <el-table v-loading="logLoading" :data="logRows" row-key="id" table-layout="fixed" empty-text="暂无库存流水">
            <el-table-column prop="logNo" label="流水号" min-width="180" show-overflow-tooltip />
            <el-table-column label="业务类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getBizTypeTagType(row.bizType)" effect="plain">
                  {{ getBizTypeLabel(row.bizType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="beforeNumber" label="变更前" width="100" />
            <el-table-column prop="changeNumber" label="变更量" width="100" />
            <el-table-column prop="afterNumber" label="变更后" width="100" />
            <el-table-column prop="operatorName" label="操作人" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="logQuery.pageNum"
              v-model:page-size="logQuery.pageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              :total="logTotal"
              @size-change="handleLogSizeChange"
              @current-change="handleLogCurrentChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog v-model="adjustDialogVisible" title="库存调整" width="520px">
      <el-descriptions v-if="currentInventory" class="detail-descriptions" :column="1" border>
        <el-descriptions-item label="物料">{{ formatEmpty(currentInventory.materialName) }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ formatEmpty(currentInventory.warehouseName) }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ formatEmpty(currentInventory.currentNumber) }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="adjustFormRef" class="adjust-form" :model="adjustForm" :rules="adjustRules" label-width="96px">
        <el-form-item label="调整数量" prop="changeNumber">
          <el-input-number v-model="adjustForm.changeNumber" :precision="3" controls-position="right" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="adjustForm.reason" :rows="4" type="textarea" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确认调整</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.adjust-form {
  margin-top: 18px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
