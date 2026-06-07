<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import {
  agentSessionDetailApi,
  agentSessionHistoryApi,
  workflowStreamApi,
  type AgentMessage,
  type AgentSession,
  type WorkflowAgentResponse,
} from '@/api/agent'
import { formatDateTime, formatEmpty } from '@/utils/format'

type ChatRole = 'user' | 'assistant'

interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  createdAt: string
  intent?: string
  data?: Record<string, unknown> | null
  pending?: boolean
  streaming?: boolean
  thinkingContent?: string
  thinkingStatus?: string
  answerStarted?: boolean
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

const route = useRoute()
const messageInput = ref('')
const sending = ref(false)
const sessionLoading = ref(false)
const detailLoading = ref(false)
const threadId = ref('')
const sessionId = ref<number>()
const messages = ref<ChatMessage[]>([])
const sessionList = ref<AgentSession[]>([])
const scrollRef = ref<HTMLElement>()
const activeStreamController = ref<AbortController | null>(null)

marked.setOptions({
  gfm: true,
  breaks: true,
})

const allQuickActions = [
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

const pageTitle = computed(() => String(route.meta.title || 'AI 智能助手'))
const pageDesc = computed(() => {
  if (route.path.includes('order-diagnosis')) {
    return '复用统一 Workflow Agent，对采购订单做阶段诊断、阻塞定位和处理建议。'
  }

  if (route.path.includes('warning')) {
    return '复用统一 Workflow Agent，扫描采购执行风险并输出汇总、重点风险和建议动作。'
  }

  if (route.path.includes('supplier-score')) {
    return '复用统一 Workflow Agent，对供应商履约进行评分并返回指标拆解和合作建议。'
  }

  return '可以自然聊天，也可以处理库存、采购、供应商和业务规则相关问题'
})

const quickActions = computed(() => {
  if (route.path.includes('order-diagnosis')) {
    return allQuickActions.filter((item) => item.title === '诊断采购订单')
  }

  if (route.path.includes('warning')) {
    return allQuickActions.filter((item) => item.title === '扫描采购风险')
  }

  if (route.path.includes('supplier-score')) {
    return allQuickActions.filter((item) => item.title === '评估供应商履约')
  }

  return allQuickActions
})

const hasMessages = computed(() => messages.value.length > 0)
const currentSession = computed(() => sessionList.value.find((item) => item.threadId === threadId.value))
const currentThreadText = computed(() => currentSession.value?.title || threadId.value || '新会话')

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function createTimeText(value?: string | null) {
  const date = value ? new Date(value) : new Date()
  if (Number.isNaN(date.getTime())) {
    return value ? formatDateTime(value) : ''
  }

  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function sessionTitle(session: AgentSession) {
  return session.title || session.threadId || '新会话'
}

function sessionTime(session: AgentSession) {
  return formatDateTime(session.lastMessageTime || session.createTime)
}

function toChatMessage(row: AgentMessage): ChatMessage | null {
  const role = row.messageRole === 'USER' ? 'user' : row.messageRole === 'ASSISTANT' ? 'assistant' : null
  if (!role || !row.content) {
    return null
  }

  return {
    id: String(row.id || createMessageId()),
    role,
    content: row.content,
    createdAt: createTimeText(row.createTime),
  }
}

function asRecord(value: unknown) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>
  }

  return null
}

function getBusinessRecord(data: unknown) {
  const record = asRecord(data)
  if (!record) {
    return null
  }

  const evidence = asRecord(record.evidence)
  if (!evidence) {
    return record
  }

  const facts = asRecord(evidence.facts) || {}
  const result: Record<string, unknown> = {
    ...record,
    ...facts,
  }

  if (typeof evidence.summary === 'string') {
    result.summary = evidence.summary
  }

  if (Array.isArray(evidence.items)) {
    result.items = evidence.items
  }

  if (Array.isArray(evidence.sourceTools)) {
    result.sourceTools = evidence.sourceTools
  }

  if (Array.isArray(evidence.errors)) {
    result.errors = evidence.errors
  }

  return result
}

function getString(data: unknown, key: string) {
  const record = getBusinessRecord(data)
  const value = record?.[key]

  if (typeof value === 'string' || typeof value === 'number') {
    return String(value)
  }

  return ''
}

function getNumber(data: unknown, key: string) {
  const record = getBusinessRecord(data)
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
  const record = getBusinessRecord(data)
  const evidence = record?.evidence

  if (Array.isArray(evidence)) {
    return evidence.map((item) => String(item))
  }

  return []
}

function getWarningItems(data: unknown) {
  const record = getBusinessRecord(data)
  const items = Array.isArray(record?.items) ? record?.items : record?.topItems

  if (!Array.isArray(items)) {
    return []
  }

  return items.map((item) => asRecord(item) || {}) as WarningItem[]
}

function getNextActionText(data: unknown) {
  const record = getBusinessRecord(data)
  const nextAction = asRecord(record?.nextAction)

  return getString(nextAction, 'actionText') || getString(record, 'suggestAction') || getString(record, 'nextAction')
}

function getResponsibilityText(data: unknown) {
  const record = getBusinessRecord(data)
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

function streamThinkingStatus(payload: unknown) {
  const stage = getString(payload, 'stage')
  const stageMap: Record<string, string> = {
    AUTH: '开始思考',
    UNDERSTAND: '正在理解你的意思',
    PLAN: '正在判断回答方式',
    EVIDENCE: '正在结合上下文',
    ANSWER: '正在组织回答',
  }

  return stageMap[stage] || '思考中'
}

function renderMarkdown(content: string) {
  const html = marked.parse(content || '', { async: false })
  return DOMPurify.sanitize(String(html))
}

async function scrollToBottom() {
  await nextTick()
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}

function applyWorkflowResponse(message: ChatMessage, result: WorkflowAgentResponse) {
  message.pending = false
  message.streaming = false
  message.thinkingStatus = '回答完成'
  message.answerStarted = true
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

async function loadSessions() {
  sessionLoading.value = true
  try {
    sessionList.value = await agentSessionHistoryApi()
  } finally {
    sessionLoading.value = false
  }
}

function refreshSessionsLater(delay = 1800) {
  window.setTimeout(() => {
    loadSessions().catch(() => undefined)
  }, delay)
}

function markStreamingMessagesInterrupted() {
  messages.value = messages.value.map((message) => {
    if (message.role !== 'assistant' || !message.streaming) {
      return message
    }

    return {
      ...message,
      pending: false,
      streaming: false,
      answerStarted: true,
      thinkingStatus: '已中断',
      content: message.content || '本次处理已中断。',
    }
  })
}

function abortActiveStream() {
  if (!activeStreamController.value) {
    return
  }

  activeStreamController.value.abort()
  activeStreamController.value = null
  sending.value = false
  markStreamingMessagesInterrupted()
}

function isAbortError(error: unknown) {
  return typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError'
}

async function openSession(session: AgentSession) {
  if (!session.threadId || detailLoading.value) {
    return
  }

  abortActiveStream()
  detailLoading.value = true
  try {
    const detail = await agentSessionDetailApi(session.threadId)
    const realSession = detail.session || session
    threadId.value = realSession.threadId || session.threadId
    sessionId.value = realSession.id
    messages.value = (detail.messages || [])
      .map(toChatMessage)
      .filter((message): message is ChatMessage => Boolean(message))
    await scrollToBottom()
  } finally {
    detailLoading.value = false
  }
}

async function sendMessage(content = messageInput.value) {
  const text = content.trim()

  if (!text || sending.value || detailLoading.value) {
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
    content: '',
    createdAt: createTimeText(),
    pending: true,
    streaming: true,
    thinkingContent: '',
    thinkingStatus: '连接中',
    answerStarted: false,
  }

  messages.value.push(assistantMessage)
  const assistantMessageId = assistantMessage.id
  const requestThreadId = threadId.value || undefined
  const streamController = new AbortController()
  activeStreamController.value = streamController
  sending.value = true
  await scrollToBottom()

  const findAssistantMessageIndex = () => messages.value.findIndex((message) => message.id === assistantMessageId)

  const updateAssistantMessage = (patch: Partial<ChatMessage>) => {
    const assistantMessageIndex = findAssistantMessageIndex()
    const current = messages.value[assistantMessageIndex]
    if (!current) {
      return
    }

    messages.value[assistantMessageIndex] = {
      ...current,
      ...patch,
    }
  }

  const appendThinkingToken = (text: string) => {
    const assistantMessageIndex = findAssistantMessageIndex()
    const current = messages.value[assistantMessageIndex]
    if (!current || !text) {
      return
    }

    messages.value[assistantMessageIndex] = {
      ...current,
      pending: false,
      thinkingContent: `${current.thinkingContent || ''}${text}`,
      thinkingStatus: '思考中',
    }
  }

  const appendAnswerToken = (text: string) => {
    const assistantMessageIndex = findAssistantMessageIndex()
    const current = messages.value[assistantMessageIndex]
    if (!current || !text) {
      return
    }

    messages.value[assistantMessageIndex] = {
      ...current,
      pending: false,
      answerStarted: true,
      content: `${current.content || ''}${text}`,
      thinkingStatus: current.thinkingContent ? '正在回答' : current.thinkingStatus || '正在回答',
    }
  }

  try {
    let hasAnswerToken = false
    let streamFailed = false
    let streamFinished = false

    await workflowStreamApi(
      {
        message: text,
        threadId: requestThreadId,
      },
      {
        onStart() {
          if (!hasAnswerToken) {
            updateAssistantMessage({
              pending: false,
              thinkingStatus: '开始思考',
            })
            scrollToBottom()
          }
        },
        onStep(payload) {
          if (!hasAnswerToken) {
            updateAssistantMessage({
              pending: false,
              thinkingStatus: streamThinkingStatus(payload),
            })
            scrollToBottom()
          }
        },
        onThinking(text) {
          appendThinkingToken(text)
          scrollToBottom()
        },
        onToken(text) {
          if (!text) {
            return
          }

          if (!hasAnswerToken) {
            hasAnswerToken = true
            updateAssistantMessage({
              pending: false,
              answerStarted: true,
              thinkingStatus: '正在回答',
            })
          }

          appendAnswerToken(text)
          scrollToBottom()
        },
        onDone(result) {
          streamFinished = true
          const assistantMessageIndex = findAssistantMessageIndex()
          const current = messages.value[assistantMessageIndex]
          if (current) {
            const nextMessage = { ...current }
            applyWorkflowResponse(nextMessage, result)
            messages.value[assistantMessageIndex] = nextMessage
          }
          scrollToBottom()
        },
        onError(message) {
          streamFinished = true
          streamFailed = true
          updateAssistantMessage({
            pending: false,
            streaming: false,
            answerStarted: true,
            thinkingStatus: '思考中断',
            content: message || '本次处理失败，请稍后重试。',
          })
          scrollToBottom()
        },
      },
      {
        signal: streamController.signal,
      },
    )

    if (!streamFinished) {
      streamFailed = true
      updateAssistantMessage({
        pending: false,
        streaming: false,
        answerStarted: true,
        thinkingStatus: '连接中断',
        content: '流式连接中断，请稍后重试。',
      })
    }

    if (!streamFailed) {
      try {
        await loadSessions()
        refreshSessionsLater()
      } catch {
        ElMessage.warning('会话列表刷新失败，请稍后手动刷新')
      }
    }
  } catch (error) {
    if (isAbortError(error)) {
      return
    }

    updateAssistantMessage({
      pending: false,
      streaming: false,
      answerStarted: true,
      thinkingStatus: '处理失败',
      content: '本次处理失败，请稍后重试。',
    })
  } finally {
    if (activeStreamController.value === streamController) {
      activeStreamController.value = null
      sending.value = false
    }
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
  abortActiveStream()
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

onMounted(() => {
  loadSessions()
})

onBeforeUnmount(() => {
  abortActiveStream()
})
</script>

<template>
  <div class="agent-page">
    <aside class="session-sidebar">
      <div class="session-brand">
        <span class="brand-mark">
          <el-icon><MagicStick /></el-icon>
        </span>
        <strong>{{ pageTitle }}</strong>
      </div>

      <el-button class="new-chat-button" @click="startNewChat">
        <el-icon><EditPen /></el-icon>
        新建会话
      </el-button>

      <div class="session-sidebar-header">
        <strong>最近</strong>
        <el-button link size="small" :loading="sessionLoading" @click="loadSessions">刷新</el-button>
      </div>

      <div v-loading="sessionLoading" class="session-list">
        <button
          v-for="session in sessionList"
          :key="session.threadId || session.id"
          class="session-item"
          :class="{ 'is-active': session.threadId === threadId }"
          type="button"
          @click="openSession(session)"
        >
          <span class="session-title">{{ sessionTitle(session) }}</span>
          <span class="session-meta">
            <span>{{ intentLabel(session.currentIntent) }}</span>
            <span>{{ sessionTime(session) }}</span>
          </span>
        </button>

        <el-empty v-if="!sessionLoading && sessionList.length === 0" :image-size="64" description="暂无历史会话" />
      </div>
    </aside>

    <section v-loading="detailLoading" class="chat-panel">
      <header class="chat-topbar">
        <div class="chat-title">
          <span>{{ currentThreadText }}</span>
        </div>
      </header>

      <main ref="scrollRef" class="chat-body" :class="{ 'is-empty': !hasMessages }">
        <section v-if="!hasMessages" class="welcome-panel">
          <div class="assistant-mark">
            <el-icon><MagicStick /></el-icon>
          </div>
          <h3>今天想聊什么？</h3>
          <p>{{ pageDesc }}</p>
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
            <div class="message-block">
              <div class="message-bubble">
                <template v-if="message.role === 'assistant'">
                  <div v-if="message.pending" class="typing">
                    <span />
                    <span />
                    <span />
                  </div>
                  <template v-else>
                    <div
                      v-if="message.thinkingContent || (message.streaming && message.thinkingStatus)"
                      class="thinking-panel"
                      :class="{ 'is-thinking': message.streaming && !message.answerStarted, 'is-done': !message.streaming }"
                    >
                      <div class="thinking-title">
                        <span class="thinking-pulse" />
                        <span>{{ message.thinkingStatus || '思考中' }}</span>
                      </div>
                      <p v-if="message.thinkingContent" class="thinking-text">{{ message.thinkingContent }}</p>
                    </div>
                    <div
                      v-if="message.content || message.answerStarted || !message.streaming"
                      class="message-text assistant-answer markdown-body"
                      v-html="renderMarkdown(message.content)"
                    />
                  </template>
                </template>
                <p v-else class="message-text">{{ message.content }}</p>
              </div>

              <div
                v-if="message.role === 'assistant' && !message.pending && !message.streaming && message.data"
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

              <div v-if="message.role === 'assistant' && !message.pending && !message.streaming" class="message-actions">
                <el-button link size="small" title="复制回答" @click="copyAnswer(message.content)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <span class="message-time">{{ message.createdAt }}</span>
                <el-tag v-if="message.intent" size="small" effect="plain">
                  {{ intentLabel(message.intent) }}
                </el-tag>
              </div>
            </div>
          </article>
        </section>
      </main>

      <footer class="composer-shell">
        <div class="composer">
          <el-input
            v-model="messageInput"
            :autosize="{ minRows: 1, maxRows: 5 }"
            type="textarea"
            resize="none"
            placeholder="有问题，尽管问"
            @keydown.enter="handleEnter"
          />
          <el-button
            class="send-button"
            type="primary"
            :loading="sending"
            :disabled="detailLoading || !messageInput.trim()"
            circle
            @click="sendMessage()"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
        <p class="composer-tip">AI 助手可能会犯错，请核查重要信息。</p>
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

.agent-workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
}

.session-sidebar {
  width: 284px;
  flex: 0 0 284px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.session-sidebar-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid var(--border-color);
}

.session-sidebar-header strong {
  color: var(--text-main);
  font-size: 14px;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 0;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
}

.session-item:hover,
.session-item:focus {
  background: #f8fafc;
}

.session-item.is-active {
  background: #eff6ff;
}

.session-title {
  max-width: 100%;
  overflow: hidden;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

.session-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  flex: 1;
  min-height: 0;
  min-width: 0;
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

.assistant-answer {
  color: #111827;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.85;
}

.thinking-panel {
  margin-bottom: 10px;
  padding-bottom: 10px;
  color: #6b7280;
  border-bottom: 1px dashed #e5e7eb;
}

.thinking-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.thinking-text {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.7;
  white-space: pre-wrap;
}

.thinking-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
}

.thinking-panel.is-thinking .thinking-pulse {
  background: var(--primary-color);
  animation: thinking-pulse 1.2s infinite ease-in-out;
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

@keyframes thinking-pulse {
  0%,
  100% {
    transform: scale(0.8);
    opacity: 0.45;
  }

  50% {
    transform: scale(1.15);
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

  .agent-workspace {
    flex-direction: column;
  }

  .session-sidebar {
    width: 100%;
    flex: 0 0 220px;
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

/* GPT-like assistant surface */
.agent-page {
  height: 100vh;
  min-height: 620px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 0;
  margin: -28px -32px;
  overflow: hidden;
  background: #ffffff;
}

.session-sidebar {
  width: auto;
  min-width: 0;
  height: 100%;
  flex: initial;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f7f7f7;
  border: 0;
  border-right: 1px solid #ececec;
  border-radius: 0;
}

.session-brand {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #111111;
  font-size: 16px;
  font-weight: 700;
}

.brand-mark {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #111111;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 15px;
}

.new-chat-button.el-button {
  height: 42px;
  justify-content: flex-start;
  gap: 10px;
  margin: 4px 12px 14px;
  padding: 0 12px;
  color: #222222;
  background: transparent;
  border: 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
}

.new-chat-button.el-button:hover,
.new-chat-button.el-button:focus {
  color: #111111;
  background: #ededed;
}

.new-chat-button.el-button :deep(> span) {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.session-sidebar-header {
  height: auto;
  padding: 8px 16px 6px;
  border-bottom: 0;
}

.session-sidebar-header strong {
  color: #4f4f4f;
  font-size: 13px;
  font-weight: 700;
}

.session-sidebar-header .el-button {
  color: #6f6f6f;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 8px 14px;
}

.session-list :deep(.el-loading-mask) {
  background: rgba(247, 247, 247, 0.75);
}

.session-item {
  width: 100%;
  min-height: 38px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  margin: 1px 0;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 10px;
}

.session-item:hover,
.session-item:focus {
  background: #eeeeee;
}

.session-item.is-active {
  background: #e9e9e9;
}

.session-title {
  max-width: 100%;
  overflow: hidden;
  color: #1f1f1f;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: none;
}

.chat-panel {
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #ffffff;
  border: 0;
  border-radius: 0;
}

.chat-topbar {
  height: 58px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.92);
}

.chat-title {
  min-width: 0;
  color: #202123;
  font-size: 15px;
  font-weight: 600;
}

.chat-title span {
  display: block;
  max-width: min(520px, 54vw);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 34px 24px 30px;
  background: #ffffff;
}

.chat-body.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-panel {
  width: min(760px, 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.assistant-mark {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #111111;
  border: 0;
  border-radius: 12px;
  font-size: 22px;
}

.welcome-panel h3 {
  margin: 18px 0 8px;
  color: #111111;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0;
}

.welcome-panel p {
  max-width: 520px;
  margin: 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
}

.quick-actions {
  width: min(680px, 100%);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 28px;
}

.quick-action.el-button {
  height: 46px;
  min-height: 46px;
  justify-content: flex-start;
  gap: 8px;
  margin-left: 0;
  padding: 0 14px;
  color: #303030;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 14px;
  box-shadow: none;
}

.quick-action.el-button + .quick-action.el-button {
  margin-left: 0;
}

.quick-action.el-button :deep(> span) {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
}

.quick-action.el-button:hover,
.quick-action.el-button:focus {
  color: #111111;
  border-color: #d4d4d4;
  background: #f7f7f7;
}

.quick-action .el-icon {
  color: #6b7280;
  font-size: 17px;
}

.message-list {
  width: min(840px, 100%);
  display: flex;
  flex-direction: column;
  gap: 28px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 0;
}

.message-row.is-user {
  justify-content: flex-end;
}

.message-row.is-assistant {
  justify-content: flex-start;
}

.message-block {
  max-width: min(760px, 100%);
}

.message-row.is-user .message-block {
  max-width: min(560px, 78%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  padding: 0;
  color: #111111;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.message-row.is-user .message-bubble {
  padding: 10px 16px;
  color: #111111;
  background: #ffe8f2;
  border: 0;
  border-radius: 22px;
}

.message-text {
  margin: 0;
  color: #111111;
  font-size: 16px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.assistant-answer {
  color: #111111;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.9;
  letter-spacing: 0;
}

.markdown-body :deep(p) {
  margin: 0 0 14px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(strong) {
  font-weight: 700;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 10px 0 14px;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(code) {
  padding: 2px 5px;
  color: #111827;
  background: #f3f4f6;
  border-radius: 5px;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  margin: 12px 0;
  padding: 12px 14px;
  overflow-x: auto;
  color: #111827;
  background: #f7f7f8;
  border: 1px solid #eeeeee;
  border-radius: 10px;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  border-radius: 0;
}

.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding-left: 14px;
  color: #4b5563;
  border-left: 3px solid #d1d5db;
}

.thinking-panel {
  margin: 0 0 14px;
  padding: 0 0 13px;
  color: #8b8b8b;
  border-bottom: 1px solid #eeeeee;
}

.thinking-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #8b8b8b;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
}

.thinking-text {
  max-height: 180px;
  margin: 8px 0 0;
  overflow-y: auto;
  color: #777777;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.75;
  white-space: pre-wrap;
}

.thinking-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b6b6b6;
}

.thinking-panel.is-thinking .thinking-pulse {
  background: #111111;
  animation: thinking-pulse 1.2s infinite ease-in-out;
}

.thinking-panel.is-done .thinking-pulse {
  display: none;
}

.typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #777777;
  animation: typing 1.1s infinite ease-in-out;
}

.structured-result {
  width: min(760px, 100%);
  margin-top: 14px;
}

.result-card {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
}

.result-card-header strong {
  color: #111111;
  font-size: 14px;
}

.result-card-header span {
  color: #6b7280;
  font-size: 13px;
}

.evidence-list {
  margin: 12px 0 0;
  padding-left: 20px;
  color: #4b5563;
  line-height: 1.8;
}

.message-actions {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  margin-top: 8px;
  color: #8b8b8b;
}

.message-actions .el-button {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #6f6f6f;
  border-radius: 8px;
}

.message-actions .el-button:hover,
.message-actions .el-button:focus {
  color: #111111;
  background: #f1f1f1;
}

.message-time {
  color: #9ca3af;
  font-size: 12px;
}

.composer-shell {
  flex: 0 0 auto;
  padding: 18px 24px 34px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, #ffffff 28%);
  border-top: 0;
}

.composer {
  width: min(780px, 100%);
  min-height: 56px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin: 0 auto;
  padding: 8px 8px 8px 12px;
  background: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 28px;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.08),
    0 12px 34px rgba(0, 0, 0, 0.08);
}

.composer:focus-within {
  border-color: #c8c8c8;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.08),
    0 14px 38px rgba(0, 0, 0, 0.1);
}

.send-button.el-button {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  margin-bottom: 2px;
  border-radius: 50%;
}

.send-button.el-button {
  color: #ffffff;
  background: #111111;
  border-color: #111111;
}

.send-button.el-button:hover,
.send-button.el-button:focus {
  background: #000000;
  border-color: #000000;
}

.send-button.el-button.is-disabled,
.send-button.el-button.is-loading {
  color: #ffffff;
  background: #d1d5db;
  border-color: #d1d5db;
}

.composer :deep(.el-textarea) {
  flex: 1;
  min-width: 0;
}

.composer :deep(.el-textarea__inner) {
  min-height: 40px !important;
  padding: 6px 0 4px;
  color: #111111;
  border: 0;
  box-shadow: none;
  font-size: 15px;
  line-height: 1.6;
}

.composer :deep(.el-textarea__inner::placeholder) {
  color: #9ca3af;
}

.composer-toolbar {
  display: none;
}

.composer-tip {
  width: min(780px, 100%);
  margin: 8px auto 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

@media (max-width: 960px) {
  .agent-page {
    height: 100vh;
    grid-template-columns: 248px minmax(0, 1fr);
    margin: -24px;
  }

  .chat-topbar {
    padding: 0 18px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .agent-page {
    height: 100vh;
    min-height: 720px;
    grid-template-columns: 1fr;
    grid-template-rows: 210px minmax(620px, 1fr);
    margin: -18px;
  }

  .session-sidebar {
    border-right: 0;
    border-bottom: 1px solid #ececec;
  }

  .session-brand {
    min-height: 48px;
  }

  .new-chat-button.el-button {
    margin-bottom: 8px;
  }

  .session-list {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 0 12px 12px;
  }

  .session-item {
    width: 180px;
    flex: 0 0 180px;
  }

  .chat-body {
    padding: 24px 16px 24px;
  }

  .message-row.is-user .message-block {
    max-width: 88%;
  }

  .message-text,
  .assistant-answer {
    font-size: 15px;
  }

  .composer-shell {
    padding: 14px 12px 26px;
  }
}
</style>
