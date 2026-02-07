import request from '@/utils/request'

// 旅行规划：解耦对话的独立接口
import { fetchEventSource } from '@microsoft/fetch-event-source'

// SSE 旅行规划，返回 AbortController 便于调用方结束
export function runTravelPlanStream(roleId, payload, { onProgress, onDone, onMessage, onError } = {}) {
  const controller = new AbortController()
  const token = localStorage.getItem('token')
  fetchEventSource(`/api/travel/plan?roleId=${roleId ?? ''}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal: controller.signal,
    retry: 0,
    onopen(res) {
      if (!res.ok) {
        controller.abort()
        onError && onError(new Error('Connection failed'))
      }
    },
    onmessage(ev) {
      const name = ev.event || 'message'
      let data = ev.data
      try { data = JSON.parse(ev.data) } catch (_) {}
      if (name === 'progress') onProgress && onProgress(data)
      else if (name === 'workflow_update') onProgress && onProgress(data)
      else if (name === 'done') {
        onDone && onDone(data)
        controller.abort()
      } else if (name === 'message') onMessage && onMessage(data)
      else onMessage && onMessage(data)
    },
    onerror(err) {
      onError && onError(err)
      controller.abort()
    }
  })
  return controller
}

