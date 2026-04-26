<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getOperLogDetailApi, getOperLogPageApi, type OperLogVO } from '@/api/log'
import { formatDateTime, formatEmpty } from '@/utils/format'

const loading = ref(false)
const detailLoading = ref(false)
const tableData = ref<OperLogVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<OperLogVO | null>(null)
const timeRange = ref<[string, string] | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  logType: '',
  operatorName: '',
  moduleName: '',
  operationType: '',
  operatorId: undefined as number | undefined,
  successFlag: undefined as number | undefined,
  beginTime: '',
  endTime: '',
})

const logTypeOptions = [
  { label: '业务日志', value: 'BUSINESS' },
  { label: '系统日志', value: 'SYSTEM' },
]

const successOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
]

function syncTimeRange() {
  query.beginTime = timeRange.value?.[0] || ''
  query.endTime = timeRange.value?.[1] || ''
}

async function loadData() {
  syncTimeRange()
  loading.value = true
  try {
    const result = await getOperLogPageApi(query)
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
    logType: '',
    operatorName: '',
    moduleName: '',
    operationType: '',
    operatorId: undefined,
    successFlag: undefined,
    beginTime: '',
    endTime: '',
  })
  timeRange.value = null
  loadData()
}

async function openDetail(row: OperLogVO) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getOperLogDetailApi(row.id)
  } finally {
    detailLoading.value = false
  }
}

function successType(flag?: number) {
  if (flag === 1) {
    return 'success'
  }

  if (flag === 0) {
    return 'danger'
  }

  return 'info'
}

function successText(flag?: number) {
  if (flag === 1) {
    return '成功'
  }
  if (flag === 0) {
    return '失败'
  }
  return '-'
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
        <h2 class="page-title">操作日志</h2>
        <p class="page-subtitle">查看关键业务操作、访问来源和执行结果</p>
      </div>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="日志类型">
            <el-select v-model="query.logType" clearable placeholder="全部类型">
              <el-option v-for="item in logTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模块名称">
            <el-input v-model="query.moduleName" clearable placeholder="输入模块" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="操作类型">
            <el-input v-model="query.operationType" clearable placeholder="输入操作类型" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model="query.operatorName" clearable placeholder="输入姓名" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="操作人编号">
            <el-input-number v-model="query.operatorId" :min="1" controls-position="right" placeholder="输入编号" />
          </el-form-item>
          <el-form-item label="执行结果">
            <el-select v-model="query.successFlag" clearable placeholder="全部结果">
              <el-option v-for="item in successOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作时间" class="time-range">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
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
          <el-table-column label="日志编号" width="96" fixed="left">
            <template #default="{ row }">{{ row.id }}</template>
          </el-table-column>
          <el-table-column label="日志类型" min-width="120">
            <template #default="{ row }">
              <span class="mono-text">{{ formatEmpty(row.logType) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="moduleName" label="模块名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="业务类型" min-width="130">
            <template #default="{ row }">
              <span class="mono-text">{{ formatEmpty(row.bizType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务记录" width="100">
            <template #default="{ row }">{{ formatEmpty(row.bizId) }}</template>
          </el-table-column>
          <el-table-column label="操作类型" min-width="130">
            <template #default="{ row }">
              <span class="mono-text operation-type">{{ formatEmpty(row.operationType) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="operationDesc" label="操作描述" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作人编号" width="120">
            <template #default="{ row }">{{ formatEmpty(row.operatorId) }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" min-width="130" show-overflow-tooltip />
          <el-table-column prop="requestUri" label="请求地址" min-width="210" show-overflow-tooltip />
          <el-table-column label="请求方式" width="100">
            <template #default="{ row }">
              <span class="mono-text">{{ formatEmpty(row.requestMethod) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源地址" min-width="150">
            <template #default="{ row }">
              <span class="mono-text">{{ formatEmpty(row.ipAddress) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="执行结果" width="100">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="successType(row.successFlag)" effect="plain">
                {{ successText(row.successFlag) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="错误信息" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.errorMessage) }}</template>
          </el-table-column>
          <el-table-column label="操作时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="日志详情" size="560px">
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <el-descriptions v-else-if="detail" class="detail-descriptions" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="日志类型">{{ formatEmpty(detail.logType) }}</el-descriptions-item>
        <el-descriptions-item label="模块名称">{{ formatEmpty(detail.moduleName) }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ formatEmpty(detail.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="业务记录">{{ formatEmpty(detail.bizId) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ formatEmpty(detail.operationType) }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ formatEmpty(detail.operationDesc) }}</el-descriptions-item>
        <el-descriptions-item label="操作人编号">{{ formatEmpty(detail.operatorId) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ formatEmpty(detail.operatorName) }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ formatEmpty(detail.requestUri) }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ formatEmpty(detail.requestMethod) }}</el-descriptions-item>
        <el-descriptions-item label="来源地址">{{ formatEmpty(detail.ipAddress) }}</el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag class="status-tag" :type="successType(detail.successFlag)" effect="plain">
            {{ successText(detail.successFlag) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ formatEmpty(detail.errorMessage) }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(detail.operateTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ formatEmpty(detail.createBy) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
.time-range {
  grid-column: span 2;
}

.time-range :deep(.el-date-editor) {
  width: 100%;
}

:deep(.el-input-number) {
  width: 100%;
}

.operation-type {
  color: var(--primary-color);
}

@media (max-width: 720px) {
  .time-range {
    grid-column: auto;
  }
}
</style>
