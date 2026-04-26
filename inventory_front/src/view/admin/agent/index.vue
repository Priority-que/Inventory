<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowExecuteApi, type WorkflowAgentResponse } from '@/api/agent'
import { formatEmpty } from '@/utils/format'

type ChatRole = 'user' | 'assistant'

interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  createdAt: string
  intent?: string
  data?: Record<string, unknown> | null
  pending?: boolean
}

interface WarningItem {
  riskLevel?: string
  bizType?: string
  bizId?: number
  bizNo?: string
  problem?: string
  reason?: string
  suggestOwner?: string
  suggestAction?: string
}

const messageInput = ref('')
const sending = ref(false)
const threadId = ref('')
const sessionId = ref<number>()
const messages = ref<ChatMessage[]>([])
const scrollRef = ref<HTMLElement>()

const quickActions = [
  {
    title: '扫描采购风险',
    icon: 'Warning',
    prompt: '请扫描最近 7 天采购执行风险，按高风险和中风险汇总，并给出建议处理角色和下一步动作。',
  },
  {
    title: '诊断采购订单',
    icon: 'Search',
    prompt: '请帮我诊断采购订单 PO202604211230001001 卡在哪个环节，并说明阻塞原因和下一步处理建议。',
  },
  {
    title: '评估供应商履约',
    icon: 'TrendCharts',
    prompt: '请对供应商 1 最近 30 天的履约表现进行评分，说明确认率、到货完成率、入库完成率和合作建议。',
  },
  {
    title: '询问业务规则',
    icon: 'Reading',
    prompt: '请说明采购订单从待确认到完成的状态流转规则。',
  },
]

const hasMessages = computed(() => messages.value.length > 0)
const currentThreadText = computed(() => threadId.value || '新会话')

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function createTimeText() {
  return new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function asRecord(value: unknown) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }

  return null
}

function getString(data: unknown, key: string) {
  const record = asRecord(data)
  const value = record?.[key]

  if (typeof value === 'string' || typeof value === 'number') {
    return String(value)
  }

  return ''
}

function getNumber(data: unknown, key: string) {
  const record = asRecord(data)
  const value = record?.[key]

  if (typeof value === 'number') {
    return value
  }

  if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
    return Number(value)
  }

  return undefined
}

function getEvidence(data: unknown) {
  const record = asRecord(data)
  const evidence = record?.evidence

  if (Array.isArray(evidence)) {
    return evidence.map((item) => String(item))
  }

  return []
}

function getWarningItems(data: unknown) {
  const record = asRecord(data)
  const items = Array.isArray(record?.items) ? record?.items : record?.topItems

  if (!Array.isArray(items)) {
    return []
  }

  return items.map((item) => asRecord(item) || {}) as WarningItem[]
}

function getNextActionText(data: unknown) {
  const record = asRecord(data)
  const nextAction = asRecord(record?.nextAction)

  return getString(nextAction, 'actionText') || getString(record, 'suggestAction') || getString(record, 'nextAction')
}

function getResponsibilityText(data: unknown) {
  const record = asRecord(data)
  const responsibility = asRecord(record?.responsibility)

  return getString(responsibility, 'ownerRoleName') || getString(record, 'suggestOwner')
}

function intentLabel(intent?: string) {
  const labelMap: Record<string, string> = {
    ORDER_DIAGNOSIS: '订单诊断',
    WARNING_SCAN: '风险扫描',
    SUPPLIER_SCORE: '供应商评分',
    KNOWLEDGE_QA: '知识问答',
    UNKNOWN: '普通对话',
  }

  return intent ? labelMap[intent] || intent : '普通对话'
}

function riskTagType(level?: string) {
  if (level === 'HIGH') {
    return 'danger'
  }

  if (level === 'MEDIUM') {
    return 'warning'
  }

  return 'info'
}

function riskText(level?: string) {
  if (level === 'HIGH') {
    return '高风险'
  }

  if (level === 'MEDIUM') {
    return '中风险'
  }

  return level || '-'
}

function levelTagType(level?: string) {
  if (level === '优秀' || level === '良好') {
    return 'success'
  }

  if (level === '一般') {
    return 'warning'
  }

  return 'info'
}

async function scrollToBottom() {
  await nextTick()
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}

function applyWorkflowResponse(message: ChatMessage, result: WorkflowAgentResponse) {
  message.pending = false
  message.content = result.answer || '已完成处理。'
  message.intent = result.intent
  message.data = result.data || null

  if (result.threadId) {
    threadId.value = result.threadId
  }

  if (result.sessionId) {
    sessionId.value = result.sessionId
  }
}

async function sendMessage(content = messageInput.value) {
  const text = content.trim()

  if (!text || sending.value) {
    return
  }

  messageInput.value = ''
  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content: text,
    createdAt: createTimeText(),
  })

  const assistantMessage: ChatMessage = {
    id: createMessageId(),
    role: 'assistant',
    content: '正在分析业务数据...',
    createdAt: createTimeText(),
    pending: true,
  }

  messages.value.push(assistantMessage)
  sending.value = true
  await scrollToBottom()

  try {
    const result = await workflowExecuteApi({
      message: text,
      threadId: threadId.value || undefined,
    })
    applyWorkflowResponse(assistantMessage, result)
  } catch {
    assistantMessage.pending = false
    assistantMessage.content = '本次处理失败，请稍后重试。'
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function handleEnter(event: KeyboardEvent) {
  if (event.shiftKey) {
    return
  }

  event.preventDefault()
  sendMessage()
}

function startNewChat() {
  messageInput.value = ''
  messages.value = []
  threadId.value = ''
  sessionId.value = undefined
}

async function copyAnswer(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持自动复制')
  }
}
</script>

<template>
  <div class="agent-page">
    <header class="page-header">
      <div>
        <h2 class="page-title">AI 智能助手</h2>
        <p class="page-desc">通过对话完成风险扫描、订单诊断、供应商评分和业务知识问答</p>
      </div>
      <div class="header-actions">
        <el-tag type="info" effect="plain">{{ currentThreadText }}</el-tag>
        <el-button @click="startNewChat">
          <el-icon><EditPen /></el-icon>
          新建会话
        </el-button>
      </div>
    </header>

    <section class="chat-panel">
      <main ref="scrollRef" class="chat-body" :class="{ 'is-empty': !hasMessages }">
        <section v-if="!hasMessages" class="welcome-panel">
          <div class="assistant-mark">
            <el-icon><MagicStick /></el-icon>
          </div>
          <h3>你想分析什么？</h3>
          <p>直接输入问题，或选择一个常用业务场景开始。</p>
          <div class="quick-actions">
            <el-button
              v-for="item in quickActions"
              :key="item.title"
              class="quick-action"
              @click="sendMessage(item.prompt)"
            >
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.title }}</span>
            </el-button>
          </div>
        </section>

        <section v-else class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="`is-${message.role}`"
          >
            <div v-if="message.role === 'assistant'" class="avatar assistant-avatar">
              <el-icon><MagicStick /></el-icon>
            </div>

            <div class="message-block">
              <div class="message-meta">
                <span>{{ message.role === 'user' ? '你' : '智能助手' }}</span>
                <span>{{ message.createdAt }}</span>
                <el-tag v-if="message.role === 'assistant' && message.intent" size="small" effect="plain">
                  {{ intentLabel(message.intent) }}
                </el-tag>
              </div>

              <div class="message-bubble">
                <div v-if="message.pending" class="typing">
                  <span />
                  <span />
                  <span />
                </div>
                <p v-else class="message-text">{{ message.content }}</p>
              </div>

              <div
                v-if="message.role === 'assistant' && !message.pending && message.data"
                class="structured-result"
              >
                <div v-if="message.intent === 'WARNING_SCAN'" class="result-card">
                  <div class="result-card-header">
                    <strong>风险扫描结果</strong>
                    <span>{{ formatEmpty(getString(message.data, 'summary')) }}</span>
                  </div>
                  <el-table :data="getWarningItems(message.data)" size="small" empty-text="暂无风险项">
                    <el-table-column label="等级" width="88">
                      <template #default="{ row }">
                        <el-tag class="status-tag" :type="riskTagType(row.riskLevel)" size="small" effect="plain">
                          {{ riskText(row.riskLevel) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="bizNo" label="业务编号" min-width="160" />
                    <el-table-column prop="problem" label="风险问题" min-width="180" show-overflow-tooltip />
                    <el-table-column prop="suggestOwner" label="处理角色" min-width="110" />
                    <el-table-column prop="suggestAction" label="建议动作" min-width="220" show-overflow-tooltip />
                  </el-table>
                </div>

                <div v-else-if="message.intent === 'SUPPLIER_SCORE'" class="result-card">
                  <div class="score-row">
                    <el-statistic title="综合评分" :value="getNumber(message.data, 'score') || 0" />
                    <el-tag
                      class="status-tag"
                      :type="levelTagType(getString(message.data, 'level'))"
                      size="large"
                      effect="plain"
                    >
                      {{ formatEmpty(getString(message.data, 'level')) }}
                    </el-tag>
                  </div>
                  <el-descriptions :column="2" border>
                    <el-descriptions-item label="供应商">{{ formatEmpty(getString(message.data, 'supplierName')) }}</el-descriptions-item>
                    <el-descriptions-item label="确认率">{{ formatEmpty(getString(message.data, 'confirmRate')) }}</el-descriptions-item>
                    <el-descriptions-item label="到货完成率">{{ formatEmpty(getString(message.data, 'arrivalCompletionRate')) }}</el-descriptions-item>
                    <el-descriptions-item label="入库完成率">{{ formatEmpty(getString(message.data, 'inboundCompletionRate')) }}</el-descriptions-item>
                    <el-descriptions-item label="异常到货率">{{ formatEmpty(getString(message.data, 'abnormalArrivalRate')) }}</el-descriptions-item>
                    <el-descriptions-item label="合作建议">{{ formatEmpty(getString(message.data, 'suggestion')) }}</el-descriptions-item>
                  </el-descriptions>
                </div>

                <div v-else-if="message.intent === 'ORDER_DIAGNOSIS'" class="result-card">
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="订单号">{{ formatEmpty(getString(message.data, 'orderNo')) }}</el-descriptions-item>
                    <el-descriptions-item label="当前阶段">{{ formatEmpty(getString(message.data, 'currentStage')) }}</el-descriptions-item>
                    <el-descriptions-item label="阻塞原因">{{ formatEmpty(getString(message.data, 'blockReason')) }}</el-descriptions-item>
                    <el-descriptions-item label="处理角色">{{ formatEmpty(getResponsibilityText(message.data)) }}</el-descriptions-item>
                    <el-descriptions-item label="建议动作">{{ formatEmpty(getNextActionText(message.data)) }}</el-descriptions-item>
                  </el-descriptions>
                  <ul v-if="getEvidence(message.data).length" class="evidence-list">
                    <li v-for="item in getEvidence(message.data)" :key="item">{{ item }}</li>
                  </ul>
                </div>
              </div>

              <div v-if="message.role === 'assistant' && !message.pending" class="message-actions">
                <el-button link size="small" @click="copyAnswer(message.content)">
                  <el-icon><CopyDocument /></el-icon>
                  复制
                </el-button>
              </div>
            </div>

            <div v-if="message.role === 'user'" class="avatar user-avatar">我</div>
          </article>
        </section>
      </main>

      <footer class="composer-shell">
        <div class="composer">
          <el-input
            v-model="messageInput"
            :autosize="{ minRows: 2, maxRows: 6 }"
            type="textarea"
            resize="none"
            placeholder="向智能助手提问，或输入“扫描最近7天采购风险”"
            @keydown.enter="handleEnter"
          />
          <div class="composer-toolbar">
            <span class="composer-tip">连续对话会复用当前会话标识；新建会话会重新开始业务上下文。</span>
            <el-button type="primary" :loading="sending" :disabled="!messageInput.trim()" @click="sendMessage()">
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.agent-page {
  height: calc(100vh - 64px);
  min-height: 640px;
  display: flex;
  flex-direction: column;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 24px;
}

.chat-body.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-panel {
  width: min(800px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.assistant-mark {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  font-size: 24px;
}

.welcome-panel h3 {
  margin: 18px 0 8px;
  color: var(--text-main);
  font-size: 28px;
  font-weight: 600;
}

.welcome-panel p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.quick-actions {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 32px;
}

.quick-action.el-button {
  height: auto;
  min-height: 82px;
  gap: 10px;
  margin-left: 0;
  padding: 14px 12px;
  color: #374151;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.quick-action.el-button + .quick-action.el-button {
  margin-left: 0;
}

.quick-action.el-button :deep(> span) {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.quick-action.el-button:hover,
.quick-action.el-button:focus {
  color: var(--primary-color);
  border-color: #bfdbfe;
  background: #f8fbff;
}

.quick-action .el-icon {
  font-size: 22px;
}

.message-list {
  width: min(980px, 100%);
  display: flex;
  flex-direction: column;
  gap: 22px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-row.is-user {
  justify-content: flex-end;
}

.avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

.assistant-avatar {
  color: var(--primary-color);
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.user-avatar {
  color: #ffffff;
  background: var(--primary-color);
}

.message-block {
  max-width: min(780px, calc(100% - 52px));
}

.message-row.is-user .message-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 7px;
  color: #9ca3af;
  font-size: 12px;
}

.message-meta span:first-child {
  color: #4b5563;
  font-weight: 600;
}

.message-bubble {
  padding: 12px 14px;
  color: var(--text-main);
  background: #f9fafb;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.message-row.is-user .message-bubble {
  color: #0f172a;
  background: #eff6ff;
  border-color: #dbeafe;
}

.message-text {
  margin: 0;
  line-height: 1.75;
  white-space: pre-wrap;
}

.typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing 1.1s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

.structured-result {
  margin-top: 10px;
}

.result-card {
  padding: 14px;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.result-card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
}

.result-card-header strong {
  color: var(--text-main);
  font-size: 15px;
}

.result-card-header span {
  color: var(--text-secondary);
  font-size: 13px;
}

.score-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.evidence-list {
  margin: 12px 0 0;
  padding-left: 20px;
  color: #4b5563;
  line-height: 1.8;
}

.message-actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 6px;
}

.composer-shell {
  flex: 0 0 auto;
  padding: 20px 24px;
  background: #f9fafb;
  border-top: 1px solid var(--border-color);
}

.composer {
  width: min(860px, 100%);
  margin: 0 auto;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.composer:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08);
}

.composer :deep(.el-textarea__inner) {
  min-height: 54px !important;
  padding: 2px 0 8px;
  color: var(--text-main);
  border: 0;
  box-shadow: none;
  font-size: 14px;
  line-height: 1.7;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-tip {
  color: var(--text-secondary);
  font-size: 12px;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }

  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@media (max-width: 920px) {
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .quick-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .composer-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .agent-page {
    height: auto;
    min-height: calc(100vh - 36px);
  }

  .chat-body {
    padding: 20px 12px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .message-block {
    max-width: calc(100% - 46px);
  }
}
</style>
