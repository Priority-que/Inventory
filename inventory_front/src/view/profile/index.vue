<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { changePasswordApi } from '@/api/auth'
import { getUserDetailApi, updateUserApi, type UserDTO, type UserVO } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { enabledStatusOptions, getOptionLabel, getOptionType, roleOptions } from '@/constants/business'
import type { ChangePasswordParams } from '@/types/auth'
import { formatDateTime, formatEmpty } from '@/utils/format'

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)
const activeTab = ref('base')
const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const detail = ref<UserVO | null>(null)

const profileForm = reactive<UserDTO>({
  id: undefined,
  name: '',
  dept: '',
  phone: '',
  email: '',
  remark: '',
})

const passwordForm = reactive<ChangePasswordParams>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const currentRoleName = computed(() => {
  const roleCode = authStore.user?.roleCodes?.[0]
  return detail.value?.roleName || roleOptions.find((item) => item.code === roleCode)?.label || '-'
})

const statusType = computed(() => getOptionType(enabledStatusOptions, detail.value?.status || authStore.user?.status))
const statusText = computed(() => getOptionLabel(enabledStatusOptions, detail.value?.status || authStore.user?.status))
const displayName = computed(() => profileForm.name || detail.value?.name || authStore.username)
const username = computed(() => detail.value?.username || authStore.user?.username || '-')

const profileRules: FormRules<UserDTO> = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

const passwordRules: FormRules<ChangePasswordParams> = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6 到 32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }

        callback()
      },
      trigger: 'blur',
    },
  ],
}

function fillForm(data: UserVO) {
  Object.assign(profileForm, {
    id: data.id,
    name: data.name || '',
    dept: data.dept || '',
    phone: data.phone || '',
    email: data.email || '',
    remark: data.remark || '',
  })
}

async function loadProfile() {
  loading.value = true
  try {
    if (!authStore.user) {
      await authStore.fetchCurrentUser()
    }

    const userId = authStore.user?.id
    if (!userId) {
      ElMessage.warning('未获取到当前用户信息')
      return
    }

    const data = await getUserDetailApi(userId)
    detail.value = data
    fillForm(data)
  } finally {
    loading.value = false
  }
}

async function submitProfile() {
  await formRef.value?.validate()
  if (!profileForm.id) {
    ElMessage.warning('未获取到当前用户信息')
    return
  }

  saving.value = true
  try {
    await updateUserApi({
      id: profileForm.id,
      name: profileForm.name,
      dept: profileForm.dept,
      phone: profileForm.phone,
      email: profileForm.email,
      remark: profileForm.remark,
    })
    ElMessage.success('个人信息已更新')
    await authStore.fetchCurrentUser()
    await loadProfile()
  } finally {
    saving.value = false
  }
}

function resetProfile() {
  if (detail.value) {
    fillForm(detail.value)
    formRef.value?.clearValidate()
  }
}

function resetPasswordForm() {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  passwordFormRef.value?.clearValidate()
}

async function submitPassword() {
  await passwordFormRef.value?.validate()
  passwordSaving.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    authStore.clearAuth()
    router.replace('/login')
  } finally {
    passwordSaving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="page profile-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">我的信息</h2>
        <p class="page-desc">维护当前账号的基础资料，便于业务协作和系统识别</p>
      </div>
    </div>

    <section v-loading="loading" class="content-panel profile-panel">
      <aside class="profile-summary">
        <div class="profile-avatar">
          <el-icon><User /></el-icon>
        </div>
        <h3>{{ displayName }}</h3>
        <p>{{ username }}</p>
        <div class="summary-tags">
          <el-tag class="status-tag" type="info" effect="plain">{{ currentRoleName }}</el-tag>
          <el-tag class="status-tag" :type="statusType" effect="plain">{{ statusText }}</el-tag>
        </div>

        <el-descriptions class="summary-descriptions" :column="1" border>
          <el-descriptions-item label="部门">{{ formatEmpty(profileForm.dept) }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ formatDateTime(detail?.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(detail?.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </aside>

      <main class="profile-form-wrap">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基础资料" name="base">
            <div class="section-heading">
              <p>用户名、角色和账号状态由系统管理；这里可维护姓名、部门、联系方式和备注。</p>
            </div>

            <el-form
              ref="formRef"
              :model="profileForm"
              :rules="profileRules"
              label-position="top"
              class="profile-form"
            >
              <div class="profile-form-grid">
                <el-form-item label="用户名">
                  <el-input :model-value="username" disabled />
                </el-form-item>
                <el-form-item label="角色">
                  <el-input :model-value="currentRoleName" disabled />
                </el-form-item>
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="profileForm.name" maxlength="30" placeholder="请输入姓名" />
                </el-form-item>
                <el-form-item label="部门">
                  <el-input v-model="profileForm.dept" maxlength="50" placeholder="请输入部门" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email" maxlength="80" placeholder="请输入邮箱" />
                </el-form-item>
              </div>

              <el-form-item label="备注">
                <el-input
                  v-model="profileForm.remark"
                  :rows="4"
                  maxlength="200"
                  show-word-limit
                  type="textarea"
                  placeholder="填写需要补充的个人说明"
                />
              </el-form-item>

              <div class="profile-actions">
                <el-button @click="resetProfile">
                  <el-icon><RefreshLeft /></el-icon>
                  重置
                </el-button>
                <el-button type="primary" :loading="saving" @click="submitProfile">
                  <el-icon><Check /></el-icon>
                  保存修改
                </el-button>
              </div>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <div class="section-heading">
              <p>请输入当前密码并设置新密码，修改成功后需要重新登录。</p>
            </div>

            <el-alert
              class="password-alert"
              title="为了账号安全，密码修改成功后当前会话会自动退出。"
              type="info"
              :closable="false"
              show-icon
            />

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
              class="password-form"
            >
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入当前密码"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  maxlength="32"
                  placeholder="请输入新密码"
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  maxlength="32"
                  placeholder="请再次输入新密码"
                  @keyup.enter="submitPassword"
                />
              </el-form-item>

              <div class="profile-actions password-actions">
                <el-button @click="resetPasswordForm">
                  <el-icon><RefreshLeft /></el-icon>
                  重置
                </el-button>
                <el-button type="primary" :loading="passwordSaving" @click="submitPassword">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-button>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </main>
    </section>
  </div>
</template>

<style scoped>
.profile-panel {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 520px;
}

.profile-summary {
  padding: 32px 24px;
  background: #f9fafb;
  border-right: 1px solid var(--border-color);
}

.profile-avatar {
  width: 56px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  font-size: 28px;
}

.profile-summary h3 {
  margin: 18px 0 6px;
  color: var(--text-main);
  font-size: 18px;
  font-weight: 600;
}

.profile-summary p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.summary-descriptions {
  margin-top: 24px;
}

.summary-descriptions :deep(.el-descriptions__label) {
  width: 88px;
  color: var(--text-secondary);
  font-weight: 500;
}

.profile-form-wrap {
  min-width: 0;
  padding: 32px 36px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-color);
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background-color: var(--primary-color);
  border-radius: 999px;
}

.profile-tabs :deep(.el-tabs__item) {
  height: 36px;
  padding: 0 22px 0 0;
  color: var(--text-secondary);
  font-size: 16px;
  font-weight: 600;
}

.profile-tabs :deep(.el-tabs__item.is-active),
.profile-tabs :deep(.el-tabs__item:hover) {
  color: var(--text-main);
}

.section-heading {
  margin-bottom: 24px;
}

.section-heading p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-form,
.password-form {
  max-width: 820px;
}

.password-form {
  max-width: 520px;
}

.password-alert {
  max-width: 520px;
  margin-bottom: 22px;
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 20px;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.password-actions {
  justify-content: flex-start;
}

@media (max-width: 980px) {
  .profile-panel {
    grid-template-columns: 1fr;
  }

  .profile-summary {
    border-right: 0;
    border-bottom: 1px solid var(--border-color);
  }
}

@media (max-width: 720px) {
  .profile-form-wrap,
  .profile-summary {
    padding: 24px;
  }

  .profile-form-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }
}
</style>
