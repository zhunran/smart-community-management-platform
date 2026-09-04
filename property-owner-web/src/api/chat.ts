import request from '@/utils/request'

export interface ChatHistoryVO {
  id: number
  sessionId: number
  role: string
  content: string
  createTime: string
}

export interface ChatSessionVO {
  id: number
  title: string
  createTime: string
  updateTime: string
}

/**
 * 流式对话（SSE）
 * 使用 fetch 而非 axios，因为 axios 不支持流式读取
 */
export function sendMessage(message: string, sessionId?: number): Promise<Response> {
  return fetch('/api/owner/chat/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ message, sessionId: sessionId ?? null }),
  })
}

/**
 * 查询指定会话的聊天历史
 */
export function getChatHistory(sessionId: number) {
  return request.get<ChatHistoryVO[]>('/api/owner/chat/history', {
    params: { sessionId },
  })
}

/**
 * 创建新会话
 */
export function createSession() {
  return request.post<ChatSessionVO>('/api/owner/chat/sessions')
}

/**
 * 获取会话列表
 */
export function getSessions() {
  return request.get<ChatSessionVO[]>('/api/owner/chat/sessions')
}

/**
 * 删除会话
 */
export function deleteSession(id: number) {
  return request.delete(`/api/owner/chat/sessions/${id}`)
}