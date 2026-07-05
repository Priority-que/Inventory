import { request } from '@/utils/request'
import { getToken } from '@/utils/auth'

export interface WorkflowAgentRequest {
  message?: string
  threadId?: string
}

export interface WorkflowAgentResponse {
  sessionId?: number
  threadId?: string
  intent?: string
  answer?: string
  data?: Record<string, unknown> | null
}

export interface WorkflowStreamPayload {
  message?: string
  stage?: string
  text?: string
  [key: string]: unknown
}

export interface WorkflowStreamHandlers {
  onStart?: (payload: WorkflowStreamPayload) => void
  onStep?: (payload: WorkflowStreamPayload) => void
  onThinking?: (text: string, payload: WorkflowStreamPayload) => void
  onToken?: (text: string, payload: WorkflowStreamPayload) => void
  onDone?: (result: WorkflowAgentResponse) => void
  onError?: (message: string, payload: WorkflowStreamPayload) => void
}

export interface WorkflowStreamOptions {
  signal?: AbortSignal
}

export interface AgentSession {
  id?: number
  sessionNo?: string
  threadId?: string
  userId?: number
  title?: string
  agentType?: string
  currentIntent?: string
  status?: string
  lastMessageTime?: string
  createTime?: string
}

export interface AgentMessage {
  id?: number
  sessionId?: number
  threadId?: string
  messageRole?: string
  messageType?: string
  content?: string
  createTime?: string
}

export interface AgentSessionDetail {
  session?: AgentSession
  messages?: AgentMessage[]
}

export function workflowExecuteApi(data: WorkflowAgentRequest) {
  return request<WorkflowAgentResponse>({
    url: '/agent/workflow/execute',
    method: 'post',
    data,
  })
}

export async function workflowStreamApi(
  data: WorkflowAgentRequest,
  handlers: WorkflowStreamHandlers = {},
  options: WorkflowStreamOptions = {},
) {
  const token = getToken()
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/agent/workflow/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
    signal: options.signal,
  })

  if (!response.ok) {
    throw new Error(`流式请求失败：HTTP ${response.status}`)
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatchEvent = (block: string) => {
    const lines = block.split(/\r?\n/)
    let eventName = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }

    if (dataLines.length === 0) {
      return
    }

    const rawData = dataLines.join('\n')
    let payload: WorkflowStreamPayload = {}

    try {
      payload = JSON.parse(rawData) as WorkflowStreamPayload
    } catch {
      payload = { message: rawData }
    }

    if (eventName === 'start') {
      handlers.onStart?.(payload)
      return
    }

    if (eventName === 'step') {
      handlers.onStep?.(payload)
      return
    }

    if (eventName === 'thinking') {
      handlers.onThinking?.(typeof payload.text === 'string' ? payload.text : '', payload)
      return
    }

    if (eventName === 'token') {
      handlers.onToken?.(typeof payload.text === 'string' ? payload.text : '', payload)
      return
    }

    if (eventName === 'done') {
      handlers.onDone?.(payload as WorkflowAgentResponse)
      return
    }

    if (eventName === 'error') {
      handlers.onError?.(String(payload.message || 'AI 助手处理失败'), payload)
    }
  }

  while (true) {
    const { done, value } = await reader.read()

    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() || ''
    blocks.forEach(dispatchEvent)
  }

  buffer += decoder.decode()

  if (buffer.trim()) {
    dispatchEvent(buffer)
  }
}

export function agentSessionHistoryApi() {
  return request<AgentSession[]>({
    url: '/agent/session/history',
    method: 'get',
  })
}

export function agentSessionDetailApi(threadId: string) {
  return request<AgentSessionDetail>({
    url: `/agent/session/history/${threadId}`,
    method: 'get',
  })
}
