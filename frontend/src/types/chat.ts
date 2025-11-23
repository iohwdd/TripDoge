// 聊天相关类型定义

// 聊天历史VO
export interface ChatHistoryVO {
  id: number
  conversationId: string
  roleId: number
  messageType: 'user' | 'assistant' | 'system'
  content: string
  inputTokens?: number
  outputTokens?: number
  createTime: string
}

// 聊天请求DTO
export interface ChatReqDTO {
  message: string
  fileId?: number
}

// 会话VO
export interface ConversationVO {
  id: string
  userId: number
  roleId: number
  createTime: string
  updateTime: string
}
