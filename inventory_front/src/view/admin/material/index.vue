<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addMaterialApi,
  deleteMaterialApi,
  getMaterialPageApi,
  updateMaterialApi,
  type MaterialDTO,
  type MaterialPageVO,
} from '@/api/material'
import { enabledStatusOptions, getOptionLabel, getOptionType } from '@/constants/business'
import { formatDateTime, formatEmpty } from '@/utils/format'

const loading = ref(false)
const tableData = ref<MaterialPageVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  code: '',
  name: '',
  specification: '',
  safetyNumber: undefined as number | undefined,
  safetyNumberBegin: undefined as number | undefined,
  upperNumber: undefined as number | undefined,
  upperNumberBegin: undefined as number | undefined,
  status: '',
})

const selectedRows = ref<MaterialPageVO[]>([])
const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const materialForm = reactive<MaterialDTO>({
  code: '',
  name: '',
  specification: '',
  unit: '',
  categoryName: '',
  safetyNumber: undefined,
  upperNumber: undefined,
  status: 'ENABLED',
  remark: '',
})

const detailVisible = ref(false)
const detail = ref<MaterialPageVO | null>(null)

const formRules: FormRules<MaterialDTO> = {
  code: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  specification: [{ required: true, message: '请输入规格型号', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await getMaterialPageApi(query)
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
    code: '',
    name: '',
    specification: '',
    safetyNumber: undefined,
    safetyNumberBegin: undefined,
    upperNumber: undefined,
    upperNumberBegin: undefined,
    status: '',
  })
  loadData()
}

function handleSelectionChange(rows: MaterialPageVO[]) {
  selectedRows.value = rows
}

function resetForm() {
  Object.assign(materialForm, {
    id: undefined,
    code: '',
    name: '',
    specification: '',
    unit: '',
    categoryName: '',
    safetyNumber: undefined,
    upperNumber: undefined,
    status: 'ENABLED',
    remark: '',
  })
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: MaterialPageVO) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(materialForm, {
    id: row.id,
    code: row.code,
    name: row.name,
    specification: row.specification,
    unit: row.unit,
    categoryName: row.categoryName,
    safetyNumber: row.safetyNumber,
    upperNumber: row.upperNumber,
    status: row.status || 'ENABLED',
    remark: row.remark,
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await addMaterialApi({ ...materialForm })
      ElMessage.success('物料已新增')
    } else {
      await updateMaterialApi({ ...materialForm })
      ElMessage.success('物料已更新')
    }
  dialogVisible.value = false
  loadData()
  } finally {
    formLoading.value = false
  }
}

function openDetail(row: MaterialPageVO) {
  detail.value = row
  detailVisible.value = true
}

async function handleDelete(ids: number[], confirmed = false) {
  if (!ids.length) {
    ElMessage.warning('请选择要删除的物料')
    return
  }

  if (!confirmed) {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 个物料吗？`, '删除物料', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  }

  await deleteMaterialApi(ids)
  ElMessage.success('物料已删除')
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
        <h2 class="page-title">物料管理</h2>
        <p class="page-subtitle">维护物料编码、规格、单位和库存阈值</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增物料
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--4" label-position="top">
          <el-form-item label="物料编码">
            <el-input v-model="query.code" clearable placeholder="输入编码" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="物料名称">
            <el-input v-model="query.name" clearable placeholder="输入名称" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="规格型号">
            <el-input v-model="query.specification" clearable placeholder="输入规格" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in enabledStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="安全库存">
            <el-input-number v-model="query.safetyNumber" :min="0" controls-position="right" placeholder="等于" />
          </el-form-item>
          <el-form-item label="安全库存下限">
            <el-input-number
              v-model="query.safetyNumberBegin"
              :min="0"
              controls-position="right"
              placeholder="不低于"
            />
          </el-form-item>
          <el-form-item label="上限库存">
            <el-input-number v-model="query.upperNumber" :min="0" controls-position="right" placeholder="等于" />
          </el-form-item>
          <el-form-item label="上限库存下限">
            <el-input-number
              v-model="query.upperNumberBegin"
              :min="0"
              controls-position="right"
              placeholder="不低于"
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
          <el-table-column prop="code" label="物料编码" min-width="130" show-overflow-tooltip />
          <el-table-column prop="name" label="物料名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="specification" label="规格型号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="categoryName" label="分类" min-width="120" show-overflow-tooltip />
          <el-table-column prop="safetyNumber" label="安全库存" min-width="110" />
          <el-table-column prop="upperNumber" label="上限库存" min-width="110" />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(enabledStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(enabledStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该物料吗？" confirm-button-text="删除" @confirm="handleDelete([row.id], true)">
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
      :title="dialogMode === 'create' ? '新增物料' : '编辑物料'"
      width="720px"
      @closed="resetForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="materialForm" :rules="formRules" label-width="96px">
        <div class="dialog-grid">
          <el-form-item label="物料编码" prop="code">
            <el-input v-model="materialForm.code" placeholder="输入物料编码" />
          </el-form-item>
          <el-form-item label="物料名称" prop="name">
            <el-input v-model="materialForm.name" placeholder="输入物料名称" />
          </el-form-item>
          <el-form-item label="规格型号" prop="specification">
            <el-input v-model="materialForm.specification" placeholder="输入规格型号" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model="materialForm.unit" placeholder="如 个、箱、米" />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="materialForm.categoryName" placeholder="输入分类名称" />
          </el-form-item>
          <el-form-item label="安全库存">
            <el-input-number v-model="materialForm.safetyNumber" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="上限库存">
            <el-input-number v-model="materialForm.upperNumber" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="materialForm.status" placeholder="选择状态">
              <el-option
                v-for="item in enabledStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="materialForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="物料详情" size="520px">
      <el-descriptions v-if="detail" class="detail-descriptions" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ formatEmpty(detail.code) }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ formatEmpty(detail.name) }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ formatEmpty(detail.specification) }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ formatEmpty(detail.unit) }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ formatEmpty(detail.categoryName) }}</el-descriptions-item>
        <el-descriptions-item label="安全库存">{{ formatEmpty(detail.safetyNumber) }}</el-descriptions-item>
        <el-descriptions-item label="上限库存">{{ formatEmpty(detail.upperNumber) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="status-tag" :type="getOptionType(enabledStatusOptions, detail.status)" effect="plain">
            {{ getOptionLabel(enabledStatusOptions, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
:deep(.el-input-number) {
  width: 100%;
}
</style>
