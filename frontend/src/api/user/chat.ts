// 聊天相关API
import { userRequest } from './request'
import type { ChatHistoryVO } from '@/types/chat'

/**
 * 获取对话历史
 * @param roleId 角色ID
 */
export const getChatHistory = (roleId: number) => {
  return userRequest.post<ChatHistoryVO[]>(`/api/chat/${roleId}/history`)
}

/**
 * 重置对话上下文
 * @param roleId 角色ID
 */
export const resetChat = (roleId: number) => {
  return userRequest.post(`/api/chat/${roleId}/reset`)
}

// 注意：SSE流式对话将在任务4.2中实现
// export const chatStream = (roleId: number, req: ChatReqDTO) => {
//   // 使用 EventSource 或 fetch stream 实现
// }

