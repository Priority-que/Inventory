<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  importRagKnowledgeApi,
  searchRagKnowledgeApi,
  type RagImportResultVO,
  type RagKnowledgeImportRequest,
  type RagSearchResultVO,
} from '@/api/knowledge'
import { getOptionLabel, intentOptions } from '@/constants/business'
import { formatEmpty } from '@/utils/format'

const importFormRef = ref<FormInstance>()
const importLoading = ref(false)
const searchLoading = ref(false)
const importResult = ref<RagImportResultVO | null>(null)
const searchResults = ref<RagSearchResultVO[]>([])
const activeTab = ref('import')

const docTypeOptions = [
  { label: '业务规则', value: 'BUSINESS_RULE' },
  { label: '操作说明', value: 'OPERATION_GUIDE' },
  { label: '常见问题', value: 'FAQ' },
  { label: '其他', value: 'OTHER' },
]

const importForm = reactive<RagKnowledgeImportRequest>({
  docCode: '',
  title: '',
  docType: 'BUSINESS_RULE',
  bizIntent: 'COMMON',
  sourcePath: 'manual',
  content: '',
})

const searchForm = reactive({
  query: '',
  bizIntent: 'COMMON',
  topK: 4,
})

const importRules: FormRules<RagKnowledgeImportRequest> = {
  docCode: [
    { required: true, message: '请输入文档编码', trigger: 'blur' },
    {
      pattern: /^[A-Z0-9_-]{3,64}$/,
      message: '文档编码仅支持大写字母、数字、下划线和中划线',
      trigger: 'blur',
    },
  ],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
}

function resetImportForm() {
  Object.assign(importForm, {
    docCode: '',
    title: '',
    docType: 'BUSINESS_RULE',
    bizIntent: 'COMMON',
    sourcePath: 'manual',
    content: '',
  })
  importResult.value = null
  importFormRef.value?.clearValidate()
}

async function submitImport() {
  await importFormRef.value?.validate()
  importLoading.value = true
  try {
    importResult.value = await importRagKnowledgeApi({ ...importForm })
    ElMessage.success('知识已导入')
  } finally {
    importLoading.value = false
  }
}

async function runSearch() {
  if (!searchForm.query) {
    ElMessage.warning('请输入检索问题')
    return
  }

  searchLoading.value = true
  try {
    searchResults.value = await searchRagKnowledgeApi({
      query: searchForm.query,
      bizIntent: searchForm.bizIntent || undefined,
      topK: searchForm.topK,
    })
  } finally {
    searchLoading.value = false
  }
}

function formatScore(score?: number) {
  if (score === undefined || score === null) {
    return '-'
  }

  return score.toFixed(3)
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库管理</h2>
        <p class="page-subtitle">导入业务知识，并验证知识召回效果</p>
      </div>
    </div>

    <section class="content-panel knowledge-panel">
      <el-tabs v-model="activeTab" class="knowledge-tabs">
        <el-tab-pane label="知识导入" name="import">
          <div class="knowledge-form-wrap">
            <el-form
              ref="importFormRef"
              v-loading="importLoading"
              :model="importForm"
              :rules="importRules"
              label-position="top"
            >
              <div class="form-grid">
                <el-form-item label="文档编码" prop="docCode">
                  <el-input v-model="importForm.docCode" placeholder="如 ORDER_STATUS_RULE_V1" />
                </el-form-item>
                <el-form-item label="标题" prop="title">
                  <el-input v-model="importForm.title" placeholder="输入知识标题" />
                </el-form-item>
                <el-form-item label="文档类型">
                  <el-select v-model="importForm.docType" placeholder="选择文档类型">
                    <el-option
                      v-for="item in docTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="适用场景">
                  <el-select v-model="importForm.bizIntent" placeholder="选择适用场景">
                    <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item class="full-row" label="来源路径">
                  <el-input v-model="importForm.sourcePath" placeholder="如 manual 或文档路径" />
                </el-form-item>
                <el-form-item class="full-row" label="正文内容" prop="content">
                  <el-input
                    v-model="importForm.content"
                    class="content-textarea"
                    :rows="10"
                    type="textarea"
                    placeholder="输入要导入的业务规则、操作说明或知识内容"
                  />
                </el-form-item>
              </div>

              <div class="knowledge-form-footer">
                <el-button @click="resetImportForm">重置</el-button>
                <el-button type="primary" :loading="importLoading" @click="submitImport">
                  <el-icon><Upload /></el-icon>
                  导入知识
                </el-button>
              </div>
            </el-form>

            <el-alert
              v-if="importResult"
              class="result-alert"
              type="success"
              show-icon
              :closable="false"
              :title="importResult.message || '知识导入完成'"
            >
              <template #default>
                <div class="import-result">
                  <span>文档编码：{{ formatEmpty(importResult.docCode) }}</span>
                  <span>标题：{{ formatEmpty(importResult.title) }}</span>
                  <span>适用场景：{{ getOptionLabel(intentOptions, importResult.bizIntent) }}</span>
                  <span>切片数量：{{ formatEmpty(importResult.chunkCount) }}</span>
                </div>
              </template>
            </el-alert>
          </div>
        </el-tab-pane>

        <el-tab-pane label="知识检索" name="search">
          <div class="knowledge-search-wrap">
            <el-form :model="searchForm" class="search-form" label-position="top">
              <el-form-item label="检索问题">
                <el-input v-model="searchForm.query" clearable placeholder="输入问题" @keyup.enter="runSearch" />
              </el-form-item>
              <el-form-item label="适用场景">
                <el-select v-model="searchForm.bizIntent" clearable placeholder="全部场景">
                  <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="返回数量">
                <el-input-number v-model="searchForm.topK" :min="1" :max="10" controls-position="right" />
              </el-form-item>
              <el-form-item class="filter-actions is-inline">
                <el-button type="primary" :loading="searchLoading" @click="runSearch">
                  <el-icon><Search /></el-icon>
                  检索
                </el-button>
              </el-form-item>
            </el-form>

            <div class="table-wrap">
              <el-table v-loading="searchLoading" :data="searchResults" table-layout="fixed" empty-text="暂无检索结果">
                <el-table-column prop="id" label="切片编号" min-width="220" show-overflow-tooltip />
                <el-table-column prop="docCode" label="文档编码" min-width="160" show-overflow-tooltip />
                <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
                <el-table-column prop="docType" label="文档类型" min-width="120" show-overflow-tooltip />
                <el-table-column label="适用场景" min-width="120">
                  <template #default="{ row }">{{ getOptionLabel(intentOptions, row.bizIntent) }}</template>
                </el-table-column>
                <el-table-column prop="sourcePath" label="来源路径" min-width="180" show-overflow-tooltip />
                <el-table-column prop="chunkNo" label="切片序号" width="100" />
                <el-table-column prop="content" label="命中内容" min-width="320" show-overflow-tooltip />
                <el-table-column label="相关度" width="100">
                  <template #default="{ row }">{{ formatScore(row.score) }}</template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.knowledge-panel {
  min-height: 620px;
}

.knowledge-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 24px;
  background: #f9fafb;
  border-bottom: 1px solid var(--border-color);
}

.knowledge-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.knowledge-tabs :deep(.el-tabs__item) {
  height: 52px;
  color: var(--text-secondary);
  font-weight: 500;
}

.knowledge-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.knowledge-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.knowledge-form-wrap {
  width: 100%;
  max-width: 800px;
  margin: 36px auto;
  padding: 0 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 24px;
}

.full-row {
  grid-column: 1 / -1;
}

.content-textarea :deep(.el-textarea__inner) {
  min-height: 240px !important;
  line-height: 1.7;
}

.knowledge-form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 22px;
  border-top: 1px solid var(--border-color);
}

.knowledge-search-wrap {
  padding: 24px;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 220px 180px auto;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-actions {
  min-width: 90px;
}

.result-alert {
  margin-top: 16px;
}

.import-result {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  margin-top: 8px;
}

@media (max-width: 960px) {
  .form-grid,
  .search-form {
    grid-template-columns: 1fr;
  }

  .full-row {
    grid-column: auto;
  }
}
</style>
