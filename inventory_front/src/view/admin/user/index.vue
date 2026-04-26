<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addUserApi,
  getUserDetailApi,
  getUserPageApi,
  resetPasswordApi,
  updateUserApi,
  updateUserRoleApi,
  updateUserStatusApi,
  type UserDTO,
  type UserVO,
} from '@/api/user'
import { enabledStatusOptions, getOptionLabel, getOptionType, roleOptions } from '@/constants/business'
import { formatDateTime, formatEmpty } from '@/utils/format'

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  roleName: '',
  dept: '',
  status: '',
})

const selectedRows = ref<UserVO[]>([])
const selectedCount = computed(() => selectedRows.value.length)

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formLoading = ref(false)
const userForm = reactive<UserDTO>({
  username: '',
  name: '',
  roleId: 2,
  phone: '',
  email: '',
  dept: '',
  remark: '',
})

const roleDialogVisible = ref(false)
const roleForm = reactive({
  userId: 0,
  name: '',
  roleId: 2,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<UserVO | null>(null)

const userRules: FormRules<UserDTO> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await getUserPageApi(query)
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
    name: '',
    roleName: '',
    dept: '',
    status: '',
  })
  loadData()
}

function handleSelectionChange(rows: UserVO[]) {
  selectedRows.value = rows
}

function resetUserForm() {
  Object.assign(userForm, {
    id: undefined,
    username: '',
    name: '',
    roleId: 2,
    phone: '',
    email: '',
    dept: '',
    remark: '',
  })
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetUserForm()
  dialogVisible.value = true
}

async function openEditDialog(row: UserVO) {
  dialogMode.value = 'edit'
  resetUserForm()
  dialogVisible.value = true
  formLoading.value = true
  try {
    const data = await getUserDetailApi(row.id)
    Object.assign(userForm, {
      id: data.id,
      username: data.username,
      name: data.name,
      phone: data.phone || '',
      email: data.email || '',
      dept: data.dept || '',
      remark: data.remark || '',
    })
  } finally {
    formLoading.value = false
  }
}

async function submitUserForm() {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await addUserApi({
        username: userForm.username,
        name: userForm.name,
        roleId: userForm.roleId,
        phone: userForm.phone,
        email: userForm.email,
        dept: userForm.dept,
        remark: userForm.remark,
      })
      ElMessage.success('用户已新增，初始密码为 123456')
    } else {
      await updateUserApi({
        id: userForm.id,
        name: userForm.name,
        phone: userForm.phone,
        email: userForm.email,
        dept: userForm.dept,
        remark: userForm.remark,
      })
      ElMessage.success('用户资料已更新')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    formLoading.value = false
  }
}

function openRoleDialog(row: UserVO) {
  const role = roleOptions.find((item) => item.label === row.roleName)
  Object.assign(roleForm, {
    userId: row.id,
    name: row.name,
    roleId: role?.value || 2,
  })
  roleDialogVisible.value = true
}

async function submitRoleForm() {
  await updateUserRoleApi({
    userId: roleForm.userId,
    roleId: roleForm.roleId,
  })
  ElMessage.success('用户角色已更新')
  roleDialogVisible.value = false
  loadData()
}

async function handleStatus(row: UserVO) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const nextText = nextStatus === 'ENABLED' ? '启用' : '禁用'

  await ElMessageBox.confirm(`确定要${nextText}“${row.name}”吗？`, `${nextText}用户`, {
    type: 'warning',
    confirmButtonText: nextText,
    cancelButtonText: '取消',
  })

  await updateUserStatusApi({
    id: row.id,
    status: nextStatus,
  })
  ElMessage.success(`用户已${nextText}`)
  loadData()
}

async function handleResetPassword(row: UserVO) {
  await ElMessageBox.confirm(`确定要重置“${row.name}”的密码吗？`, '重置密码', {
    type: 'warning',
    confirmButtonText: '重置',
    cancelButtonText: '取消',
  })

  await resetPasswordApi(row.id)
  ElMessage.success('密码已重置为 123456')
}

async function openDetail(row: UserVO) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getUserDetailApi(row.id)
  } finally {
    detailLoading.value = false
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
        <h2 class="page-title">用户管理</h2>
        <p class="page-subtitle">维护账号基础资料、状态和主角色</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>

    <section class="content-panel">
      <div class="filter-section">
        <el-form :model="query" class="user-toolbar-form" label-position="top">
          <el-form-item label="姓名">
            <el-input v-model="query.name" clearable placeholder="输入姓名" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="query.roleName" clearable placeholder="全部角色">
              <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="部门">
            <el-input v-model="query.dept" clearable placeholder="输入部门" @keyup.enter="handleSearch" />
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
          <el-form-item class="filter-actions is-inline">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="batch-action-bar">
        <span>已选 <strong class="batch-count">{{ selectedCount }}</strong> 项</span>
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
          <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
          <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
          <el-table-column prop="roleName" label="角色" min-width="130" show-overflow-tooltip />
          <el-table-column prop="dept" label="部门" min-width="130" show-overflow-tooltip />
          <el-table-column prop="phone" label="手机" min-width="130" />
          <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="getOptionType(enabledStatusOptions, row.status)" effect="plain">
                {{ getOptionLabel(enabledStatusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatEmpty(row.remark) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-dropdown trigger="click">
                  <el-button link type="primary">
                    更多
                    <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="openRoleDialog(row)">调整角色</el-dropdown-item>
                      <el-dropdown-item @click="handleStatus(row)">
                        {{ row.status === 'ENABLED' ? '禁用用户' : '启用用户' }}
                      </el-dropdown-item>
                      <el-dropdown-item divided @click="handleResetPassword(row)">重置密码</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="680px"
      @closed="resetUserForm"
    >
      <el-form ref="formRef" v-loading="formLoading" :model="userForm" :rules="userRules" label-width="96px">
        <div class="dialog-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" :disabled="dialogMode === 'edit'" placeholder="输入用户名" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="userForm.name" placeholder="输入姓名" />
          </el-form-item>
          <el-form-item v-if="dialogMode === 'create'" label="角色" prop="roleId">
            <el-select v-model="userForm.roleId" placeholder="选择角色">
              <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机">
            <el-input v-model="userForm.phone" placeholder="输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="userForm.email" placeholder="输入邮箱" />
          </el-form-item>
          <el-form-item label="部门">
            <el-input v-model="userForm.dept" placeholder="输入部门" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="userForm.remark" :rows="3" type="textarea" placeholder="填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="formLoading" @click="submitUserForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="调整角色" width="420px">
      <el-form :model="roleForm" label-width="84px">
        <el-form-item label="用户">
          <el-input v-model="roleForm.name" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleId" placeholder="选择角色">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRoleForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="用户详情" size="480px">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-descriptions v-else-if="detail" class="detail-descriptions" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ formatEmpty(detail.roleName) }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ formatEmpty(detail.phone) }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ formatEmpty(detail.email) }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ formatEmpty(detail.dept) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="status-tag" :type="getOptionType(enabledStatusOptions, detail.status)" effect="plain">
            {{ getOptionLabel(enabledStatusOptions, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ formatDateTime(detail.lastLoginTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ formatEmpty(detail.remark) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
.user-toolbar-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 16px;
  align-items: flex-start;
}

.user-toolbar-form :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 1180px) {
  .user-toolbar-form {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 720px) {
  .user-toolbar-form {
    grid-template-columns: 1fr;
  }
}
</style>
