import axios from 'axios'
import { Message } from '@arco-design/web-vue'
import router from '@/router'

const service = axios.create({
  baseURL: '/api', 
  timeout: 30000 // 稍微调大超时时间，以防AI生成慢
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 二进制流直接返回 (用于下载)
    if (response.config.responseType === 'blob') {
        return response
    }

    // 如果是 EventSource/SSE，通常不走这个拦截器，而是用 fetch/EventSource API。
    // 但如果是普通JSON请求：
    if (res.code !== 200) {
      Message.error(res.message || 'Error')

      // 10105: 用户未登录, 10106: 会话过期
      if (res.code === 10105 || res.code === 10106) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res.data
    }
  },
  error => {
    // 只有在有响应内容时才尝试显示后端错误消息
    const msg = error.response?.data?.message || error.message || 'Request Error'
    Message.error(msg)
    return Promise.reject(error)
  }
)

export default service
