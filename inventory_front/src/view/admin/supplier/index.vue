<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadProps,
  type UploadUserFile,
} from 'element-plus'
import {
  addSupplierApi,
  deleteSupplierApi,
  getSupplierPageApi,
  updateSupplierApi,
  uploadSupplierFileApi,
  type SupplierDTO,
  type SupplierVO,
} from '@/api/supplier'
import { fileTypeOptions, getOptionLabel, getOptionType, supplierStatusOptions } from '@/constants/business'
import { formatDateTime, formatEmpty } from '@/utils/format'

const loading = ref(false)
const tableData = ref<SupplierVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  code: '',
  name: '',
  contactName: '',
  contactPhone: '',
  status: '',
})

const selectedRows = ref<SupplierVO[]>([])
const selectedIds = computed(() => selectedRows.value.map((item) => item.id))

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const supplierForm = reactive<SupplierDTO>({
  userId: undefined,
  code: '',
  name: '',
  contactName: '',
  contactPhone: '',
  email: '',
  address: '',
  licenseNo: '',
  remark: '',
})

const uploadDialogVisible = ref(false)
const uploadLoading = ref(false)
const fileList = ref<UploadUserFile[]>([])
const currentFile = ref<File | null>(null)
const uploadForm = reactive({
  supplierId: 0,
  supplierName: '',
  fileType: 'business_license',
  remark: '',
})

const detailVisible = ref(false)
const detail = ref<SupplierVO | null>(null)

const formRules: FormRules<SupplierDTO> = {
  code: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await getSupplierPageApi(query)
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
    contactName: '',
    contactPhone: '',
    status: '',
  })
  loadData()
}

function handleSelectionChange(rows: SupplierVO[]) {
  selectedRows.value = rows
}

function resetForm() {
  Object.assign(supplierForm, {
    id: undefined,
    userId: undefined,
    code: '',
    name: '',
    contactName: '',
    contactPhone: '',
    email: '',
    address: '',
    licenseNo: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: SupplierVO) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(supplierForm, {
    id: row.id,
    userId: row.userId,
    code: row.code,
    name: row.name,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    email: row.email,
    address: row.address,
    licenseNo: row.licenseNo,
    remark: row.remark,
  })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await addSupplierApi({ ...supplierForm })
      ElMessage.success('供应商已新增')
    } else {
      await updateSupplierApi({ ...supplierForm })
      ElMessage.success('供应商资料已更新')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    formLoading.value = false
  }
}

async function handleDelete(ids: number[], confirmed = false) {
  if (!ids.length) {
    ElMessage.warning('请选择要删除的供应商')
    return
  }

  if (!confirmed) {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 个供应商吗？`, '删除供应商', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  }

  await deleteSupplierApi(ids)
  ElMessage.success('供应商已删除')
  loadData()
}

function openUploadDialog(row: SupplierVO) {
  Object.assign(uploadForm, {
    supplierId: row.id,
    supplierName: row.name || '',
    fileType: 'business_license',
    remark: '',
  })
  fileList.value = []
  currentFile.value = null
  uploadDialogVisible.value = true
}

const handleFileChange: UploadProps['onChange'] = (uploadFile) => {
  currentFile.value = uploadFile.raw || null
}

const handleFileRemove: UploadProps['onRemove'] = () => {
  currentFile.value = null
}

const handleFileExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning('每次只能上传一个附件')
}

async function submitUpload() {
  if (!currentFile.value) {
    ElMessage.warning('请先选择附件')
    return
  }

  uploadLoading.value = true
  try {
    const fileId = await uploadSupplierFileApi({
      supplierId: uploadForm.supplierId,
      fileType: uploadForm.fileType,
      remark: uploadForm.remark,
      file: currentFile.value,
    })
    ElMessage.success(`附件已上传，编号 ${fileId}`)
    uploadDialogVisible.value = false
    loadData()
  } finally {
    uploadLoading.value = false
  }
}

function openDetail(row: SupplierVO) {
  detail.value = row
  detailVisible.value = true
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
        <h2 class="page-title">供应商管理</h2>
        <p class="page-subtitle">维护供应商档案、联系人和资质附件</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增供应商
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="filter-grid filter-grid--3" label-position="top">
          <el-form-item label="供应商编码">
            <el-input v-model="query.code" clearable placeholder="输入编码" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="供应商名称">
            <el-input v-model="query.name" clearable placeholder="输入名称" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="query.contactName" clearable placeholder="输入联系人" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="query.contactPhone" clearable placeholder="输入电话" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option
                v-for="item in supplierStatusOptions"
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
          <el-table-column label="绑定用户" width="96">
            <template #default="{ row }">{{ formatEmpty(row.userId) }}</template>
          </el-table-column>
          <el-table-column prop="code" label="供应商编码" min-width="130" show-overflow-tooltip />
          <el-table-column prop="name" label="供应商名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="contactName" label="联系人" min-width="120" show-overflow-tooltip />
          <el-table-column prop="contactPhone" label="联系电话" min-width="130" />
          <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
          <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
          <el-table-column prop="licenseNo" label="营业执照号" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(supplierStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(supplierStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="附件数量" width="96">
            <template #default="{ row }">{{ formatEmpty(row.fileRound) }}</template>
          </el-table-column>
          <el-table-column label="审核备注" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.reviewNote) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="primary" @click="openUploadDialog(row)">附件</el-button>
                <el-popconfirm
                  title="确定删除该供应商吗？"
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
      :title="dialogMode === 'create' ? '新增供应商' : '编辑供应商'"
      width="760px"
      @closed="resetForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="supplierForm" :rules="formRules" label-width="112px">
        <div class="dialog-grid">
          <el-form-item label="绑定用户">
            <el-input-number v-model="supplierForm.userId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="供应商编码" prop="code">
            <el-input v-model="supplierForm.code" placeholder="输入供应商编码" />
          </el-form-item>
          <el-form-item label="供应商名称" prop="name">
            <el-input v-model="supplierForm.name" placeholder="输入供应商名称" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="supplierForm.contactName" placeholder="输入联系人" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="supplierForm.contactPhone" placeholder="输入联系电话" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="supplierForm.email" placeholder="输入邮箱" />
          </el-form-item>
          <el-form-item label="营业执照号">
            <el-input v-model="supplierForm.licenseNo" placeholder="输入营业执照号" />
          </el-form-item>
        </div>
        <el-form-item label="地址">
          <el-input v-model="supplierForm.address" placeholder="输入地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="supplierForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" title="上传供应商附件" width="520px">
      <el-form :model="uploadForm" label-width="96px">
        <el-form-item label="供应商">
          <el-input v-model="uploadForm.supplierName" disabled />
        </el-form-item>
        <el-form-item label="附件类型">
          <el-select v-model="uploadForm.fileType" placeholder="选择附件类型">
            <el-option v-for="item in fileTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" placeholder="填写备注" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            v-model:file-list="fileList"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-exceed="handleFileExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或点击选择</div>
            <template #tip>
              <div class="el-upload__tip">单个文件最大 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="供应商详情" size="520px">
      <el-descriptions v-if="detail" class="detail-descriptions" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="绑定用户">{{ formatEmpty(detail.userId) }}</el-descriptions-item>
        <el-descriptions-item label="供应商编码">{{ formatEmpty(detail.code) }}</el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ formatEmpty(detail.name) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ formatEmpty(detail.contactName) }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ formatEmpty(detail.contactPhone) }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ formatEmpty(detail.email) }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ formatEmpty(detail.address) }}</el-descriptions-item>
        <el-descriptions-item label="营业执照号">{{ formatEmpty(detail.licenseNo) }}</el-descriptions-item>
        <el-descriptions-item label="附件轮次">{{ formatEmpty(detail.fileRound) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="status-tag" :type="getOptionType(supplierStatusOptions, detail.status)" effect="plain">
            {{ getOptionLabel(supplierStatusOptions, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核备注">{{ formatEmpty(detail.reviewNote) }}</el-descriptions-item>
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
