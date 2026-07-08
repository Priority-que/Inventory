<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect)
  } catch {
    // 统一错误提示由 request 拦截器处理，登录页只负责收起 loading。
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <header class="login-hero">
      <span class="brand-mark">
        <el-icon><DataAnalysis /></el-icon>
      </span>
      <h1>库存管理系统</h1>
      <p>采购 · 库存 · 仓储 · 供应商协同</p>
    </header>

    <section class="login-panel">
      <div class="login-brand">
        <h2>欢迎回来</h2>
        <p>登录账号进入业务工作台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleLogin">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="账号" clearable>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
          <el-icon><Position /></el-icon>
          登录系统
        </el-button>
      </el-form>
    </section>

    <footer class="login-footer">© 2026 库存管理系统</footer>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 26px;
  padding: 44px 24px 34px;
  background:
    linear-gradient(135deg, rgba(219, 234, 254, 0.84) 0%, rgba(248, 250, 252, 0.94) 42%, rgba(236, 254, 255, 0.9) 100%),
    var(--app-bg);
}

.login-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.brand-mark {
  width: 58px;
  height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: var(--primary-color);
  border: 4px solid rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  box-shadow: 0 18px 36px rgba(15, 98, 254, 0.18);
  font-size: 26px;
}

.login-hero h1 {
  margin: 18px 0 0;
  color: #4ea3dd;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 0;
}

.login-hero p {
  margin: 9px 0 0;
  color: #6b7d95;
  font-size: 15px;
  font-weight: 500;
}

.login-panel {
  width: min(520px, 100%);
  padding: 42px 44px 46px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 22px;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
}

.login-brand {
  margin-bottom: 30px;
  text-align: center;
}

.login-brand h2 {
  margin: 0;
  color: #0f172a;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: 0;
}

.login-brand p {
  margin: 8px 0 0;
  color: #8090a6;
  font-size: 15px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  min-height: 52px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 13px;
  box-shadow: 0 0 0 1px #dbe5ef inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #bfdbfe inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #93c5fd inset,
    0 14px 28px rgba(37, 99, 235, 0.1);
}

:deep(.el-input__prefix),
:deep(.el-input__suffix) {
  color: #9aaec6;
}

:deep(.el-input__inner) {
  color: #0f172a;
  font-size: 15px;
}

:deep(.el-input__inner::placeholder) {
  color: #9aaec6;
}

.login-submit {
  width: 100%;
  height: 54px;
  margin-top: 12px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(90deg, #75a7f8 0%, #54b8df 100%);
  box-shadow: 0 16px 30px rgba(45, 117, 217, 0.18);
  font-size: 16px;
  font-weight: 700;
}

.login-submit:hover,
.login-submit:focus {
  background: linear-gradient(90deg, #6198f5 0%, #3faed6 100%);
  box-shadow: 0 18px 34px rgba(45, 117, 217, 0.22);
}

.login-submit .el-icon {
  margin-right: 8px;
}

.login-footer {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 640px) {
  .login-page {
    gap: 20px;
    padding: 28px 16px;
  }

  .login-hero h1 {
    font-size: 28px;
  }

  .login-panel {
    padding: 32px 22px 34px;
    border-radius: 18px;
  }
}
</style>
