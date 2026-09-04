<template>
  <div class="chat-page">
    <!-- 抽屉遮罩 -->
    <div
      class="drawer-overlay"
      :class="{ visible: drawerOpen }"
      @click="closeDrawer"
      @touchmove.prevent
    />

    <!-- 左侧抽屉：历史会话 -->
    <div class="drawer" :class="{ open: drawerOpen }">
      <div class="drawer-header">
        <span class="drawer-title">对话历史</span>
        <span class="drawer-close" @click="closeDrawer">✕</span>
      </div>
      <div class="drawer-body">
        <div class="new-chat-btn" @click="handleNewChat">
          <span class="new-chat-icon">+</span>
          <span>新建对话</span>
        </div>
        <div class="session-list">
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === currentSessionId }"
            @click="switchSession(s)"
          >
            <div class="session-info">
              <div class="session-title">{{ s.title }}</div>
              <div class="session-time">{{ formatSessionTime(s.updateTime) }}</div>
            </div>
            <span class="session-delete" @click.stop="handleDeleteSession(s)">🗑</span>
          </div>
          <div v-if="sessions.length === 0" class="no-sessions">
            暂无对话记录
          </div>
        </div>
      </div>
    </div>

    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <div class="top-left">
        <span class="back-btn" @click="goBack" aria-label="返回">←</span>
        <span class="menu-btn" @click="openDrawer">☰</span>
      </div>
      <span class="top-title">{{ currentTitle }}</span>
      <span class="new-btn" @click="handleNewChat">✚</span>
    </div>

    <!-- 消息区域 -->
    <div class="msg-area" ref="msgAreaRef">
      <!-- 欢迎页 -->
      <div v-if="messages.length === 0 && !loading" class="welcome-area">
        <div class="welcome-card">
          <div class="welcome-avatar">
            <svg viewBox="0 0 64 64" class="ai-icon">
              <defs>
                <linearGradient id="aiGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#667eea;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#764ba2;stop-opacity:1" />
                </linearGradient>
              </defs>
              <circle cx="32" cy="32" r="30" fill="url(#aiGrad)" />
              <text x="32" y="40" text-anchor="middle" fill="white" font-size="28" font-weight="bold">AI</text>
            </svg>
          </div>
          <div class="welcome-title">AI 智慧社区助手</div>
          <div class="welcome-desc">物业管理 · 缴费咨询 · 报修服务 · 社区公告</div>
        </div>
        <div class="quick-cards">
          <div class="quick-card" v-for="q in quickQuestions" :key="q" @click="quickSend(q)">
            <span class="quick-icon">{{ q.icon }}</span>
            <span class="quick-text">{{ q.text }}</span>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['msg-row', msg.role === 'user' ? 'msg-user' : 'msg-ai']"
      >
        <div class="msg-avatar" v-if="msg.role === 'assistant'">
          <svg viewBox="0 0 32 32" class="avatar-svg">
            <circle cx="16" cy="16" r="15" fill="url(#aiGrad)" />
            <text x="16" y="21" text-anchor="middle" fill="white" font-size="13" font-weight="bold">AI</text>
          </svg>
        </div>
        <div class="msg-bubble">
          <div class="msg-content" v-html="formatContent(msg.content)" />
        </div>
        <div class="msg-avatar" v-if="msg.role === 'user'">
          <div class="avatar-user">👤</div>
        </div>
      </div>

      <!-- 打字动画：仅在尚未收到任何内容时显示 -->
      <div v-if="loading && !aiStarted" class="msg-row msg-ai">
        <div class="msg-avatar">
          <svg viewBox="0 0 32 32" class="avatar-svg">
            <circle cx="16" cy="16" r="15" fill="url(#aiGrad)" />
            <text x="16" y="21" text-anchor="middle" fill="white" font-size="13" font-weight="bold">AI</text>
          </svg>
        </div>
        <div class="msg-bubble typing-bubble">
          <span class="dot" />
          <span class="dot" />
          <span class="dot" />
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-area">
      <div class="input-wrapper">
        <input
          ref="inputRef"
          v-model="inputText"
          class="msg-input"
          placeholder="输入您的问题..."
          :disabled="loading"
          @keyup.enter="send"
          @compositionstart="composing = true"
          @compositionend="composing = false"
        />
        <button
          class="send-btn"
          :class="{ active: inputText.trim() && !loading }"
          :disabled="loading || !inputText.trim()"
          @click="send"
        >
          <svg viewBox="0 0 24 24" class="send-icon">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendMessage, getChatHistory, createSession, getSessions, deleteSession } from '@/api/chat'
import type { ChatSessionVO } from '@/api/chat'

interface Message {
  role: string
  content: string
  createTime: string
}

const router = useRouter()
const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const aiStarted = ref(false)
const composing = ref(false)
const msgAreaRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

const drawerOpen = ref(false)
const sessions = ref<ChatSessionVO[]>([])
const currentSessionId = ref<number | null>(null)
const currentTitle = ref('AI 物业客服')

const quickQuestions = [
  { icon: '💰', text: '怎么缴纳物业费？' },
  { icon: '🔧', text: '如何报修？' },
  { icon: '🅿️', text: '车位怎么租？' },
  { icon: '📋', text: '本月物业费多少？' },
]

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length > 0) {
    await switchSession(sessions.value[0])
  }
})

async function loadSessions() {
  try {
    const res = await getSessions()
    if (res.data) {
      sessions.value = res.data
    }
  } catch { /* ignore */ }
}

async function switchSession(session: ChatSessionVO) {
  currentSessionId.value = session.id
  currentTitle.value = session.title
  closeDrawer()
  try {
    const res = await getChatHistory(session.id)
    if (res.data) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch {
    messages.value = []
  }
}

async function handleNewChat() {
  try {
    const res = await createSession()
    if (res.data) {
      sessions.value.unshift(res.data)
      currentSessionId.value = res.data.id
      currentTitle.value = res.data.title
      messages.value = []
      closeDrawer()
      nextTick(() => inputRef.value?.focus())
    }
  } catch { /* ignore */ }
}

async function handleDeleteSession(session: ChatSessionVO) {
  try {
    await deleteSession(session.id)
    sessions.value = sessions.value.filter(s => s.id !== session.id)
    if (currentSessionId.value === session.id) {
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0])
      } else {
        currentSessionId.value = null
        currentTitle.value = 'AI 物业客服'
        messages.value = []
      }
    }
  } catch { /* ignore */ }
}

function openDrawer() {
  drawerOpen.value = true
}

function closeDrawer() {
  drawerOpen.value = false
}

function goBack() {
  // 有上一条路由历史则后退，否则兜底回首页（应对“直达/刷新进入”场景）
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/')
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgAreaRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function quickSend(q: { icon: string; text: string }) {
  if (loading.value) return
  inputText.value = q.text
  send()
}

async function send() {
  if (loading.value || composing.value) return
  const text = inputText.value.trim()
  if (!text) return

  // 如果还没有会话，自动创建
  if (!currentSessionId.value) {
    try {
      const res = await createSession()
      if (res.data) {
        sessions.value.unshift(res.data)
        currentSessionId.value = res.data.id
        currentTitle.value = res.data.title
      }
    } catch {
      return
    }
  }

  messages.value.push({
    role: 'user',
    content: text,
    createTime: new Date().toISOString(),
  })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  aiStarted.value = false
  const aiIdx = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    createTime: new Date().toISOString(),
  })

  try {
    const response = await sendMessage(text, currentSessionId.value!)
    if (!response.ok) throw new Error('请求失败')

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    if (!reader) throw new Error('流不可用')

    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const chunk = line.substring(5).trim()
          if (chunk) {
            // 过滤中英文双引号
            const cleaned = chunk.replace(/["""]/g, '')
            messages.value[aiIdx].content += cleaned
            if (!aiStarted.value) {
              aiStarted.value = true
            }
            scrollToBottom()
          }
        }
      }
    }

    // 刷新会话列表（标题可能已更新）
    await loadSessions()
    const s = sessions.value.find(s => s.id === currentSessionId.value)
    if (s) currentTitle.value = s.title
  } catch {
    messages.value[aiIdx].content = '抱歉，回复出了点问题，请稍后重试。'
  } finally {
    loading.value = false
    aiStarted.value = false
    scrollToBottom()
  }
}

function formatContent(text: string) {
  // 过滤中英文双引号
  return text.replace(/["""]/g, '').replace(/\n/g, '<br/>')
}

function formatSessionTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<style scoped>
/* ========== 全局变量 ========== */
.chat-page {
  --primary: #667eea;
  --primary-dark: #5a6fd6;
  --bg: #f0f2f5;
  --card: #ffffff;
  --text: #1a1a2e;
  --text-secondary: #666;
  --border: #e8e8e8;
  --user-bubble: #667eea;
  --ai-bubble: #ffffff;
  --shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  background: var(--bg);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ========== 抽屉遮罩 ========== */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 100;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.25s, visibility 0.25s;
}
.drawer-overlay.visible {
  opacity: 1;
  visibility: visible;
}

/* ========== 左侧抽屉 ========== */
.drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  max-width: 80vw;
  background: #fff;
  z-index: 101;
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.1);
}
.drawer.open {
  transform: translateX(0);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.drawer-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}
.drawer-close {
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg);
  border-radius: 12px;
  color: var(--primary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  margin-bottom: 12px;
  transition: background 0.2s;
}
.new-chat-btn:active {
  background: #e0e3f0;
}
.new-chat-icon {
  font-size: 20px;
  font-weight: 300;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  gap: 8px;
}
.session-item:active,
.session-item.active {
  background: #f0f1fe;
}
.session-info {
  flex: 1;
  min-width: 0;
}
.session-title {
  font-size: 14px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.session-delete {
  font-size: 14px;
  cursor: pointer;
  opacity: 0.5;
  flex-shrink: 0;
}
.session-delete:active {
  opacity: 1;
}

.no-sessions {
  text-align: center;
  color: #bbb;
  font-size: 14px;
  padding: 32px 0;
}

/* ========== 顶部导航栏 ========== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--card);
  flex-shrink: 0;
  z-index: 10;
  box-shadow: var(--shadow);
}
.top-left {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.menu-btn,
.back-btn,
.new-btn {
  font-size: 20px;
  color: var(--primary);
  cursor: pointer;
  padding: 4px 8px;
  user-select: none;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.top-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
  max-width: 60%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 消息区域 ========== */
.msg-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  -webkit-overflow-scrolling: touch;
}

/* ========== 欢迎页 ========== */
.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10vh;
}

.welcome-card {
  text-align: center;
  margin-bottom: 32px;
}
.welcome-avatar {
  margin-bottom: 16px;
}
.ai-icon {
  width: 72px;
  height: 72px;
}
.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 8px;
}
.welcome-desc {
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
}

.quick-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;
  max-width: 340px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--card);
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: var(--shadow);
}
.quick-card:active {
  transform: scale(0.97);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
}
.quick-icon {
  font-size: 22px;
  flex-shrink: 0;
}
.quick-text {
  font-size: 13px;
  color: var(--text);
  line-height: 1.4;
}

/* ========== 消息行 ========== */
.msg-row {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 8px;
  animation: msgIn 0.3s ease-out;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-user {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
}
.avatar-svg {
  width: 32px;
  height: 32px;
}
.avatar-user {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8ecf4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.msg-bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
  position: relative;
}
.msg-user .msg-bubble {
  background: var(--user-bubble);
  color: #fff;
  border-bottom-right-radius: 6px;
}
.msg-ai .msg-bubble {
  background: var(--ai-bubble);
  color: var(--text);
  border-bottom-left-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.msg-content {
  white-space: pre-wrap;
}

/* 打字动画 */
.typing-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 18px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #bbb;
  animation: bounce 1.4s infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1.1); }
}

/* ========== 底部输入区 ========== */
.input-area {
  padding: 10px 14px;
  padding-bottom: max(10px, env(safe-area-inset-bottom));
  background: var(--card);
  flex-shrink: 0;
  box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.04);
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: var(--bg);
  border-radius: 26px;
  padding: 4px 4px 4px 18px;
  border: 1.5px solid transparent;
  transition: border-color 0.2s;
}
.input-wrapper:focus-within {
  border-color: var(--primary);
}

.msg-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  padding: 8px 0;
  color: var(--text);
}
.msg-input::placeholder {
  color: #bbb;
}
.msg-input:disabled {
  opacity: 0.6;
}

.send-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: #d4d8e8;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s, transform 0.15s;
}
.send-btn.active {
  background: var(--primary);
}
.send-btn.active:active {
  transform: scale(0.92);
  background: var(--primary-dark);
}
.send-btn:disabled {
  cursor: not-allowed;
}
.send-icon {
  width: 20px;
  height: 20px;
}
</style>