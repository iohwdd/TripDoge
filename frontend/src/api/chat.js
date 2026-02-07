import request from '@/utils/request'

export function getChatHistory(roleId, page = 1, pageSize = 20) {
  return request({
    url: `/chat/${roleId}/history`,
    method: 'post',
    params: { page, pageSize }
  })
}

export function resetChat(roleId) {
  return request({
    url: `/chat/${roleId}/reset`,
    method: 'post'
  })
}

// 发送消息 (流式)
// 注意：在真实SSE中，通常使用 EventSource 或 fetch stream，
// 但这里我们用 axios 发送普通 POST 请求来模拟（配合 Mock）
export function sendChatMessage(roleId, data) {
  return request({
    url: `/chat/${roleId}`,
    method: 'post',
    data
  })
}

