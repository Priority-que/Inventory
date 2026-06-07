<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getInventoryPageApi, type InventoryPageVO } from '@/api/inventory'
import { formatDateTime, formatEmpty } from '@/utils/format'

const loading = ref(false)
const tableData = ref<InventoryPageVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialCode: '',
  materialName: '',
  warehouseName: '',
  stockStatus: '',
})

const stockStatusOptions = [
  { label: '正常', value: 'NORMAL', type: 'success' },
  { label: '低库存', value: 'LOW', type: 'warning' },
  { label: '超储', value: 'OVER', type: 'danger' },
] as const

function getStockStatusLabel(value?: string) {
  return stockStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

function getStockStatusType(value?: string) {
  return stockStatusOptions.find((item) => item.value === value)?.type || 'info'
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

onMounted(loadData)
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
        <el-table v-loading="loading" :data="tableData" row-key="id" table-layout="fixed">
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
  </div>
</template>