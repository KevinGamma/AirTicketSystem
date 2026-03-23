<template>
  <div class="flight-assistant-page">
    <header class="page-header">
      <div class="container">
        <div class="header-content">
          <div class="page-title-section">
            <h1 class="page-title">AI 智能航班助手</h1>
            <p class="page-subtitle">
              支持自然语言问票、多轮追问、航班推荐与流式打字回复。
            </p>
          </div>
        </div>
      </div>
    </header>

    <section class="assistant-panel-section">
      <div class="container">
        <div class="assistant-shell">
          <div class="assistant-toolbar">
            <div class="toolbar-left">
              <div class="status-badge" :class="{ busy: isStreaming }">
                <span class="status-dot"></span>
                <span>{{ isStreaming ? '助手处理中' : '会话已就绪' }}</span>
              </div>
              <div class="session-badge">
                会话 ID：<span>{{ sessionId }}</span>
              </div>
            </div>

            <div class="toolbar-actions">
              <el-button
                plain
                class="toolbar-btn"
                :loading="isResetting"
                @click="resetConversation"
              >
                <el-icon><RefreshLeft /></el-icon>
                开启新对话
              </el-button>
            </div>
          </div>

          <div ref="chatViewportRef" class="chat-viewport">
            <transition-group name="message-fade" tag="div" class="message-list">
              <article
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="`message-${message.role}`"
              >
                <div class="message-avatar">
                  {{ message.role === 'assistant' ? 'AI' : '我' }}
                </div>

                <div class="message-main">
                  <div class="message-meta">
                    <span class="message-role">
                      {{ message.role === 'assistant' ? '航班助手' : '用户' }}
                    </span>
                    <span class="message-time">{{ formatMessageTime(message.createdAt) }}</span>
                  </div>

                  <div class="message-bubble">
                    <div
                      v-if="message.role === 'assistant' && isPendingAssistantMessage(message)"
                      class="thinking-state"
                    >
                      <div class="thinking-text">助手正在思考...</div>
                      <div class="thinking-wave" aria-hidden="true">
                        <span></span>
                        <span></span>
                        <span></span>
                      </div>
                      <div v-if="currentToolHint" class="tool-hint">
                        {{ currentToolHint }}
                      </div>
                    </div>

                    <p v-else-if="message.role === 'user'" class="user-message-text">
                      {{ message.content }}
                    </p>

                    <div
                      v-else
                      class="assistant-message markdown-body"
                      v-html="renderMarkdown(message.content)"
                    ></div>
                  </div>
                </div>
              </article>
            </transition-group>

            <div ref="bottomAnchorRef" class="bottom-anchor"></div>
          </div>

          <div v-if="showQuickPrompts" class="quick-prompts">
            <div class="quick-prompts-title">你可以这样问：</div>
            <div class="quick-prompts-grid">
              <button
                v-for="prompt in quickPrompts"
                :key="prompt"
                type="button"
                class="prompt-chip"
                @click="sendPresetPrompt(prompt)"
              >
                {{ prompt }}
              </button>
            </div>
          </div>

          <div v-if="itineraryActionCards.length" class="flight-actions-panel">
            <div class="flight-actions-header">
              <div class="flight-actions-title">可直接预订的推荐航班</div>
              <div class="flight-actions-subtitle">
                这些按钮基于后端真实查询结果生成，可直接跳转到购票页。
              </div>
            </div>

            <div class="flight-actions-list">
              <article
                v-for="flight in itineraryActionCards"
                :key="flight.cardKey"
                class="flight-action-card"
              >
                <div class="flight-action-main">
                  <div class="flight-action-topline">
                    <span class="flight-action-number">{{ flight.flightNumber || '待确认航班' }}</span>
                    <span class="flight-action-airline">{{ flight.airlineName || '未知航司' }}</span>
                  </div>

                  <div class="flight-action-route">
                    <div class="flight-action-point">
                      <div class="flight-action-code">{{ flight.departureAirportCode || '--' }}</div>
                      <div class="flight-action-time">
                        {{ formatDisplayTime(flight.departureTimeLocal || flight.departureTimeUtc) }}
                      </div>
                    </div>

                    <div class="flight-action-arrow">→</div>

                    <div class="flight-action-point">
                      <div class="flight-action-code">{{ flight.arrivalAirportCode || '--' }}</div>
                      <div class="flight-action-time">
                        {{ formatDisplayTime(flight.arrivalTimeLocal || flight.arrivalTimeUtc) }}
                      </div>
                    </div>
                  </div>

                  <div class="flight-action-meta">
                    <span>日期：{{ latestFlightSearch?.departureDate || '--' }}</span>
                    <span>票价：{{ formatFlightPrice(flight.price) }}</span>
                    <span>余票：{{ formatSeatCount(flight.availableSeats) }}</span>
                  </div>
                </div>

                <div class="flight-action-buttons">
                  <el-button plain class="toolbar-btn" @click="goToFlightSearch">
                    查看搜索页
                  </el-button>
                  <el-button type="primary" class="send-btn" @click="goToBooking(flight)">
                    立即购票
                  </el-button>
                </div>
              </article>
            </div>
          </div>

          <div class="composer-card">
            <div class="composer-header">
              <div class="composer-title">向助手提问</div>
              <div class="composer-tip">
                {{ isStreaming ? '回复生成中，请稍候' : '支持“明天”“下周五”“魔都”等自然表达。' }}
              </div>
            </div>

            <el-input
              v-model="composerText"
              type="textarea"
              resize="none"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="例如：我后天想从魔都去伦敦，帮我推荐几个合适的航班。"
              class="assistant-input"
              :disabled="isStreaming"
              @keydown.enter.exact.prevent="sendMessage"
            />

            <div class="composer-actions">
              <div class="composer-status">
                <span v-if="currentToolHint && isStreaming">{{ currentToolHint }}</span>
                <span v-else-if="isStreaming">正在接收流式回复...</span>
                <span v-else>按 Enter 发送，Shift + Enter 换行。</span>
              </div>

              <div class="composer-buttons">
                <el-button
                  plain
                  class="toolbar-btn"
                  :disabled="isStreaming || !composerText.trim()"
                  @click="composerText = ''"
                >
                  清空输入
                </el-button>
                <el-button
                  type="primary"
                  class="send-btn"
                  :loading="isStreaming"
                  :disabled="!composerText.trim() || isStreaming"
                  @click="sendMessage"
                >
                  <el-icon><Promotion /></el-icon>
                  发送消息
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, RefreshLeft } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import api from '../api'

const TYPEWRITER_BATCH_SIZE = 1
const TYPEWRITER_DELAY_MS = 42
const TYPEWRITER_PUNCTUATION_DELAY_MS = 120

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

const router = useRouter()

const messages = ref([])
const composerText = ref('')
const sessionId = ref('')
const isStreaming = ref(false)
const isResetting = ref(false)
const currentToolHint = ref('')
const latestFlightSearch = ref(null)
const chatViewportRef = ref(null)
const bottomAnchorRef = ref(null)
const activeAbortController = ref(null)
const typingBuffer = ref('')
const typingTimerId = ref(null)

const quickPrompts = [
  '我明天想从上海去东京，帮我推荐上午出发的航班。',
  '我想去伦敦，但还没确定日期，你先帮我梳理需要哪些信息。',
  '帮我比较一下后天从北京到纽约的几个航班。',
  '如果当天没有直飞，可以帮我看看是否值得接受中转吗？'
]

const showQuickPrompts = computed(() => messages.value.length <= 1 && !isStreaming.value)
const itineraryActionCards = computed(() => {
  const itineraries = latestFlightSearch.value?.itineraries
  if (Array.isArray(itineraries) && itineraries.length) {
    return itineraries.slice(0, 3).map(normalizeItineraryCard)
  }

  const flights = latestFlightSearch.value?.flights
  return Array.isArray(flights)
    ? flights.slice(0, 3).map((flight) => normalizeItineraryCard({
        type: 'DIRECT',
        segmentIds: flight?.id ? [flight.id] : [],
        flightNumber: flight?.flightNumber,
        airlineName: flight?.airlineName,
        departureAirportCode: flight?.departureAirportCode,
        departureTimeLocal: flight?.departureTimeLocal,
        departureTimeUtc: flight?.departureTimeUtc,
        arrivalAirportCode: flight?.arrivalAirportCode,
        arrivalTimeLocal: flight?.arrivalTimeLocal,
        arrivalTimeUtc: flight?.arrivalTimeUtc,
        price: flight?.price,
        availableSeats: flight?.availableSeats
      }))
    : []
})

function createSessionId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '').slice(0, 12)
  }
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`.slice(0, 12)
}

function createMessage(role, content = '') {
  return {
    id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role,
    content,
    rawContent: content,
    createdAt: Date.now()
  }
}

function buildWelcomeMessage() {
  return createMessage(
    'assistant',
    [
      '你好，我是你的 **AI 智能航班助手**。',
      '',
      '你可以直接告诉我：',
      '',
      '- 出发地和目的地',
      '- 时间偏好，例如“明天”“下周五”“国庆假期”',
      '- 对价格、直飞、中转、到达时间的要求',
      '',
      '我会结合后端真实航班数据给你推荐，不会编造航班信息。'
    ].join('\n')
  )
}

function formatMessageTime(timestamp) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(timestamp)
}

function renderMarkdown(content) {
  return markdown.render(content || '')
}

function sanitizeAssistantContent(rawContent) {
  if (!rawContent) {
    return ''
  }

  return rawContent
    .replace(/\}\s*<\/tool_call>/gi, '')
    .replace(/\}\s*<\/tool_response>/gi, '')
    .replace(/<tool_call>[\s\S]*?<\/tool_call>/gi, '')
    .replace(/<tool_response>[\s\S]*?<\/tool_response>/gi, '')
    .replace(/<tool_call>[\s\S]*$/gi, '')
    .replace(/<tool_response>[\s\S]*$/gi, '')
    .replace(/<\/?tool_call>/gi, '')
    .replace(/<\/?tool_response>/gi, '')
    .replace(/^[\t ]*(?:\{|\}|\[|\]|,)+[\t ]*$/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trimEnd()
}

function appendAssistantDelta(assistantMessage, payload) {
  if (!payload) {
    return
  }

  typingBuffer.value = `${typingBuffer.value}${payload}`
  scheduleTypewriter(assistantMessage)
}

function normalizeItineraryCard(itinerary) {
  const segments = Array.isArray(itinerary?.segments) ? itinerary.segments.filter(Boolean) : []
  const segmentIds = Array.isArray(itinerary?.segmentIds) && itinerary.segmentIds.length
    ? itinerary.segmentIds.filter(Boolean)
    : segments.map((segment) => segment?.id).filter(Boolean)
  const flightNumbers = segments.map((segment) => segment?.flightNumber).filter(Boolean)
  const airlines = Array.from(new Set(segments.map((segment) => segment?.airlineName).filter(Boolean)))
  const firstSegment = segments[0]
  const lastSegment = segments[segments.length - 1]
  const transferCities = segments
    .slice(0, -1)
    .map((segment) => segment?.arrivalCity || segment?.arrivalAirportCode)
    .filter(Boolean)

  return {
    ...itinerary,
    cardKey: segmentIds.length
      ? segmentIds.join('-')
      : `${itinerary?.flightNumber || itinerary?.flightNumberSummary || 'itinerary'}-${itinerary?.departureTimeUtc || Date.now()}`,
    segmentIds,
    flightNumber:
      itinerary?.flightNumber ||
      itinerary?.flightNumberSummary ||
      flightNumbers.join(' + '),
    airlineName: itinerary?.airlineName || airlines.join(' / '),
    departureAirportCode: itinerary?.departureAirportCode || firstSegment?.departureAirportCode,
    departureTimeLocal: itinerary?.departureTimeLocal || firstSegment?.departureTimeLocal,
    departureTimeUtc: itinerary?.departureTimeUtc || firstSegment?.departureTimeUtc,
    arrivalAirportCode: itinerary?.arrivalAirportCode || lastSegment?.arrivalAirportCode,
    arrivalTimeLocal: itinerary?.arrivalTimeLocal || lastSegment?.arrivalTimeLocal,
    arrivalTimeUtc: itinerary?.arrivalTimeUtc || lastSegment?.arrivalTimeUtc,
    price: itinerary?.price ?? itinerary?.totalPrice,
    availableSeats: itinerary?.availableSeats,
    connectionSummary: itinerary?.connectionInfo || (transferCities.length ? `经 ${transferCities.join(' / ')} 中转` : '')
  }
}

function formatFlightPrice(price) {
  if (price === null || price === undefined || price === '') {
    return '待确认'
  }
  return `¥${price}`
}

function formatSeatCount(seats) {
  if (seats === null || seats === undefined) {
    return '未知'
  }
  return `${seats}`
}

function formatDisplayTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ')
}

function takeLeadingCharacters(text, count) {
  const characters = Array.from(text || '')
  return {
    chunk: characters.slice(0, count).join(''),
    rest: characters.slice(count).join('')
  }
}

function scrollToBottom(behavior = 'smooth') {
  nextTick(() => {
    bottomAnchorRef.value?.scrollIntoView({
      behavior,
      block: 'end'
    })
  })
}

function stopTypewriter() {
  if (typingTimerId.value !== null) {
    window.clearTimeout(typingTimerId.value)
    typingTimerId.value = null
  }
  typingBuffer.value = ''
}

function flushAssistantChunk(assistantMessage, chunk) {
  if (!chunk) {
    return
  }

  assistantMessage.rawContent = `${assistantMessage.rawContent || ''}${chunk}`
  assistantMessage.content = sanitizeAssistantContent(assistantMessage.rawContent)
  scrollToBottom('auto')
}

function scheduleTypewriter(assistantMessage) {
  if (typingTimerId.value !== null) {
    return
  }

  const run = () => {
    if (!typingBuffer.value) {
      typingTimerId.value = null
      return
    }

    const { chunk, rest } = takeLeadingCharacters(typingBuffer.value, TYPEWRITER_BATCH_SIZE)
    typingBuffer.value = rest
    flushAssistantChunk(assistantMessage, chunk)

    if (!typingBuffer.value) {
      typingTimerId.value = null
      return
    }

    const lastChar = Array.from(chunk).at(-1) || ''
    const delay = /[，。！？；：,.!?;:]/.test(lastChar)
      ? TYPEWRITER_PUNCTUATION_DELAY_MS
      : TYPEWRITER_DELAY_MS

    typingTimerId.value = window.setTimeout(run, delay)
  }

  typingTimerId.value = window.setTimeout(run, TYPEWRITER_DELAY_MS)
}

function waitForTypewriterDrain() {
  return new Promise((resolve) => {
    const poll = () => {
      if (!typingBuffer.value && typingTimerId.value === null) {
        resolve()
        return
      }

      window.setTimeout(poll, TYPEWRITER_DELAY_MS)
    }

    poll()
  })
}

function isPendingAssistantMessage(message) {
  const lastMessage = messages.value[messages.value.length - 1]
  return isStreaming.value && message.role === 'assistant' && message.id === lastMessage?.id && !message.content
}

function resolveToolHint(toolName) {
  const toolLabelMap = {
    searchFlights: '助手正在调用后端航班检索接口...'
  }
  return toolLabelMap[toolName] || `助手正在执行工具：${toolName}`
}

async function sendPresetPrompt(prompt) {
  if (isStreaming.value) {
    return
  }
  composerText.value = prompt
  await sendMessage()
}

async function sendMessage() {
  const question = composerText.value.trim()
  if (!question || isStreaming.value) {
    return
  }

  const userMessage = createMessage('user', question)
  const assistantMessage = createMessage('assistant', '')

  stopTypewriter()
  messages.value.push(userMessage, assistantMessage)
  composerText.value = ''
  isStreaming.value = true
  currentToolHint.value = ''
  latestFlightSearch.value = null
  scrollToBottom('smooth')

  const abortController = new AbortController()
  activeAbortController.value = abortController

  try {
    const headers = {
      'Content-Type': 'application/json'
    }

    const token = localStorage.getItem('token')
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(`${api.defaults.baseURL}/ai/flight-assistant/stream`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        sessionId: sessionId.value,
        message: question
      }),
      signal: abortController.signal
    })

    if (!response.ok || !response.body) {
      const errorText = await response.text()
      throw new Error(errorText || '流式接口调用失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    let streamCompleted = false
    while (!streamCompleted) {
      const { done, value } = await reader.read()
      if (done) {
        streamCompleted = true
        continue
      }

      buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '')

      let separatorIndex = buffer.indexOf('\n\n')
      while (separatorIndex !== -1) {
        const rawEventBlock = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex + 2)
        handleSseBlock(rawEventBlock, assistantMessage)
        separatorIndex = buffer.indexOf('\n\n')
      }
    }

    if (buffer.trim()) {
      handleSseBlock(buffer, assistantMessage)
    }

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = '抱歉，本次没有收到有效回复，请稍后重试。'
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      return
    }

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = `抱歉，助手暂时不可用。\n\n原因：${error.message || '未知异常'}`
    }
    ElMessage.error(error.message || '助手请求失败')
  } finally {
    await waitForTypewriterDrain()
    activeAbortController.value = null
    isStreaming.value = false
    currentToolHint.value = ''
    scrollToBottom('smooth')
  }
}

function handleSseBlock(rawEventBlock, assistantMessage) {
  if (!rawEventBlock.trim()) {
    return
  }

  let eventName = 'message'
  const dataLines = []

  rawEventBlock.split('\n').forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
      return
    }

    if (line.startsWith('data: ')) {
      dataLines.push(line.slice(6))
      return
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5))
    }
  })

  handleSseEvent(eventName, dataLines.join('\n'), assistantMessage)
}

function handleSseEvent(eventName, payload, assistantMessage) {
  switch (eventName) {
    case 'session':
      if (payload) {
        sessionId.value = payload
      }
      break

    case 'tool':
      currentToolHint.value = resolveToolHint(payload)
      break

    case 'flight_search_result':
      try {
        latestFlightSearch.value = JSON.parse(payload)
      } catch (error) {
        console.error('Failed to parse flight search result:', error)
      }
      scrollToBottom('smooth')
      break

    case 'delta':
      appendAssistantDelta(assistantMessage, payload)
      scrollToBottom('smooth')
      break

    case 'usage':
    case 'done':
      currentToolHint.value = ''
      break

    case 'error':
      throw new Error(payload || '后端返回了错误事件')

    default:
      if (payload) {
        appendAssistantDelta(assistantMessage, payload)
      }
      break
  }
}

function goToBooking(flight) {
  const segmentIds = Array.isArray(flight?.segmentIds)
    ? flight.segmentIds.filter(Boolean)
    : flight?.id
      ? [flight.id]
      : []

  if (!segmentIds.length) {
    ElMessage.warning('当前航班缺少可跳转的购票标识')
    return
  }

  router.push({
    path: '/booking',
    query: {
      flightId: String(segmentIds[0]),
      ...(segmentIds.length > 1
        ? {
            connectingFlightIds: segmentIds.join(','),
            isConnecting: 'true'
          }
        : {}),
      passengers: '1'
    }
  })
}

function goToFlightSearch() {
  router.push({
    path: '/flights',
    query: {
      from: latestFlightSearch.value?.departure?.canonicalQuery || '',
      to: latestFlightSearch.value?.arrival?.canonicalQuery || '',
      date: latestFlightSearch.value?.departureDate || ''
    }
  })
}

async function resetConversation() {
  if (isResetting.value) {
    return
  }

  isResetting.value = true

  try {
    stopTypewriter()
    activeAbortController.value?.abort()

    if (sessionId.value) {
      await api.delete(`/ai/flight-assistant/sessions/${encodeURIComponent(sessionId.value)}`)
    }
  } catch (error) {
    ElMessage.warning(error.response?.data?.message || '后端会话清理失败，已为你重建本地会话')
  } finally {
    sessionId.value = createSessionId()
    messages.value = [buildWelcomeMessage()]
    latestFlightSearch.value = null
    composerText.value = ''
    currentToolHint.value = ''
    isStreaming.value = false
    isResetting.value = false
    scrollToBottom('auto')
  }
}

onMounted(() => {
  sessionId.value = createSessionId()
  messages.value = [buildWelcomeMessage()]
  latestFlightSearch.value = null
  scrollToBottom('auto')
})

onBeforeUnmount(() => {
  stopTypewriter()
  activeAbortController.value?.abort()
})
</script>

<style scoped>
.flight-assistant-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.page-header {
  padding: 2rem 0 1.5rem;
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
}

.header-content {
  text-align: center;
}

.page-title {
  margin: 0 0 0.75rem;
  font-size: 2rem;
  font-weight: 700;
  color: #333333;
}

.page-subtitle {
  margin: 0;
  font-size: 1.125rem;
  color: #666666;
}

.assistant-panel-section {
  flex: 1;
  padding: 0 0 2rem;
  margin-top: -1rem;
}

.assistant-shell {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid var(--color-border-primary);
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.assistant-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-6);
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  border-bottom: 1px solid var(--color-border-primary);
}

.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.status-badge,
.session-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 12px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.status-badge {
  color: var(--color-success);
  background: var(--color-success-light);
}

.status-badge.busy {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.session-badge {
  color: var(--color-text-secondary);
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--color-border-primary);
}

.session-badge span {
  font-family: var(--font-family-monospace);
  color: var(--color-text-primary);
}

.toolbar-btn {
  border-radius: var(--radius-md);
}

.chat-viewport {
  flex: 1;
  height: 58vh;
  min-height: 460px;
  max-height: 760px;
  overflow-y: auto;
  padding: var(--space-6);
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #ffffff 24%);
}

.chat-viewport::-webkit-scrollbar {
  width: 8px;
}

.chat-viewport::-webkit-scrollbar-thumb {
  background: var(--color-border-secondary);
  border-radius: 999px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.message-item {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.message-user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  box-shadow: var(--shadow-sm);
}

.message-assistant .message-avatar {
  color: var(--color-primary);
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.message-user .message-avatar {
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
}

.message-main {
  max-width: min(78%, 880px);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.message-user .message-main {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  padding: 0 4px;
}

.message-role {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
}

.message-bubble {
  padding: var(--space-4) var(--space-5);
  border-radius: 20px;
  box-shadow: var(--shadow-md);
  line-height: var(--line-height-relaxed);
}

.message-assistant .message-bubble {
  color: var(--color-text-primary);
  background: #ffffff;
  border: 1px solid var(--color-border-primary);
  border-top-left-radius: 8px;
}

.message-user .message-bubble {
  color: var(--color-text-inverse);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  border-top-right-radius: 8px;
}

.user-message-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking-state {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 240px;
}

.thinking-text {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.thinking-wave {
  display: inline-flex;
  align-items: flex-end;
  gap: 6px;
  height: 24px;
}

.thinking-wave span {
  width: 6px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--color-primary-light), var(--color-primary));
  animation: wave 1s ease-in-out infinite;
}

.thinking-wave span:nth-child(1) {
  height: 10px;
}

.thinking-wave span:nth-child(2) {
  height: 18px;
  animation-delay: 0.15s;
}

.thinking-wave span:nth-child(3) {
  height: 12px;
  animation-delay: 0.3s;
}

.tool-hint {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 6px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-lighter);
}

.quick-prompts,
.flight-actions-panel {
  padding: 0 var(--space-6) var(--space-5);
  background: #ffffff;
}

.quick-prompts-title,
.flight-actions-title {
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
}

.quick-prompts-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.prompt-chip {
  padding: 10px 14px;
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-pill);
  background: #ffffff;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.4;
  cursor: pointer;
  transition: var(--transition-fast);
}

.prompt-chip:hover {
  color: var(--color-primary);
  border-color: rgba(37, 99, 235, 0.28);
  background: var(--color-primary-lighter);
  transform: translateY(-1px);
}

.flight-actions-header {
  margin-bottom: var(--space-3);
}

.flight-actions-subtitle {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.flight-actions-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.flight-action-card {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding: 16px;
  border: 1px solid var(--color-border-primary);
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.flight-action-main {
  min-width: 0;
  flex: 1;
}

.flight-action-topline {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.flight-action-number {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.flight-action-airline,
.flight-action-meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.flight-action-route {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}

.flight-action-point {
  min-width: 0;
}

.flight-action-code {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.flight-action-time {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.flight-action-arrow {
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}

.flight-action-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.flight-action-buttons {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.composer-card {
  padding: var(--space-5) var(--space-6) var(--space-6);
  background: #ffffff;
  border-top: 1px solid var(--color-border-primary);
}

.composer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.composer-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.composer-tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.composer-actions {
  margin-top: var(--space-4);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
}

.composer-status {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.composer-buttons {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.send-btn {
  min-width: 120px;
  border-radius: var(--radius-md);
}

.bottom-anchor {
  height: 1px;
}

.markdown-body {
  color: inherit;
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 0 0 12px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 12px;
  padding-left: 20px;
}

.markdown-body :deep(li + li) {
  margin-top: 4px;
}

.markdown-body :deep(code) {
  font-family: var(--font-family-monospace);
  font-size: 0.92em;
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.06);
}

.markdown-body :deep(pre) {
  margin: 0 0 12px;
  padding: 14px 16px;
  overflow-x: auto;
  border-radius: 12px;
  background: #0f172a;
  color: #f8fafc;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0 12px;
  overflow: hidden;
  border: 1px solid var(--color-border-primary);
  border-radius: 12px;
  background: #ffffff;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border-primary);
  text-align: left;
  font-size: var(--font-size-sm);
}

.markdown-body :deep(th) {
  background: #f8fafc;
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: none;
}

.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.18s ease;
}

.message-fade-enter-from,
.message-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

:deep(.assistant-input .el-textarea__inner) {
  min-height: 120px;
  border-radius: 16px;
  border-color: var(--color-border-primary);
  box-shadow: none;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.7;
  transition: var(--transition-fast);
}

:deep(.assistant-input .el-textarea__inner:hover) {
  border-color: var(--color-border-hover);
}

:deep(.assistant-input .el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

@keyframes wave {
  0%,
  100% {
    transform: scaleY(0.7);
    opacity: 0.6;
  }

  50% {
    transform: scaleY(1.15);
    opacity: 1;
  }
}

@media (max-width: 1023px) {
  .assistant-toolbar,
  .composer-actions,
  .composer-header,
  .flight-action-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-main {
    max-width: 88%;
  }

  .chat-viewport {
    height: 62vh;
    min-height: 420px;
  }

  .flight-action-buttons {
    width: 100%;
    flex-direction: row;
  }
}

@media (max-width: 767px) {
  .page-title {
    font-size: 1.5rem;
  }

  .assistant-toolbar,
  .chat-viewport,
  .quick-prompts,
  .flight-actions-panel,
  .composer-card {
    padding-left: var(--space-4);
    padding-right: var(--space-4);
  }

  .message-item {
    gap: var(--space-2);
  }

  .message-avatar {
    width: 36px;
    height: 36px;
    border-radius: 12px;
    font-size: 12px;
  }

  .message-main {
    max-width: calc(100% - 48px);
  }

  .message-bubble {
    padding: 12px 14px;
  }

  .composer-buttons,
  .flight-action-buttons {
    width: 100%;
    justify-content: flex-end;
  }

  .quick-prompts-grid {
    flex-direction: column;
  }

  .prompt-chip {
    width: 100%;
    text-align: left;
  }
}
</style>
