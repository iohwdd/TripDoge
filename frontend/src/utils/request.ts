import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { message } from 'antd'
import { ENV } from '@/constants/env'
import { tokenStorage, clearStorage } from './storage'
import type { ApiResponse, RequestConfig } from '@/types/api'
import { ErrorCode } from '@/types/api'

// 创建Axios实例
const createAxiosInstance = (baseURL: string): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // 请求拦截器
  instance.interceptors.request.use(
    config => {
      // 添加Token
      const token = tokenStorage.get()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    error => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { data } = response

      // 如果code不是200，说明业务逻辑错误
      if (data.code !== ErrorCode.SUCCESS) {
        // 处理特殊错误码
        if (data.code === ErrorCode.UNAUTHORIZED) {
          // Token过期，清除存储并跳转登录
          clearStorage()
          window.location.href = '/user/login'
          message.error('登录已过期，请重新登录')
          return Promise.reject(new Error(data.message || '登录已过期'))
        }

        // 用户未登录错误码（10105），静默处理，不显示错误提示
        // 这个错误码通常出现在应用启动时检查登录状态，属于正常情况
        if (data.code === ErrorCode.USER_NOT_LOGIN) {
          return Promise.reject(new Error(data.message || '用户未登录'))
        }

        // 显示错误信息
        message.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message || '请求失败'))
      }

      return response
    },
    (error: AxiosError) => {
      // 处理HTTP错误
      if (error.response) {
        const { status, data } = error.response

        switch (status) {
          case ErrorCode.UNAUTHORIZED:
            clearStorage()
            window.location.href = '/user/login'
            message.error('登录已过期，请重新登录')
            break
          case ErrorCode.FORBIDDEN:
            message.error('没有权限访问')
            break
          case ErrorCode.NOT_FOUND:
            message.error('请求的资源不存在')
            break
          case ErrorCode.SERVER_ERROR:
            message.error('服务器错误，请稍后重试')
            break
          default:
            message.error((data as { message?: string })?.message || '请求失败，请稍后重试')
        }
      } else if (error.request) {
        // 请求已发出但没有收到响应（可能是后端未启动或网络问题）
        const apiUrl = ENV.API_BASE_URL
        message.error(`无法连接到后端服务器（${apiUrl}），请检查后端服务是否已启动`)
        console.error('API请求失败:', {
          url: error.config?.url,
          baseURL: error.config?.baseURL,
          message: error.message,
        })
      } else {
        // 请求配置错误
        message.error('请求配置错误')
      }

      return Promise.reject(error)
    }
  )

  return instance
}

// 创建用户端API实例
export const userApi = createAxiosInstance(ENV.API_BASE_URL)

// 创建管理端API实例（如果需要不同的baseURL可以单独配置）
export const adminApi = createAxiosInstance(ENV.API_BASE_URL)

// 封装请求方法
export const request = {
  get: <T = unknown>(
    url: string,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return userApi.get<ApiResponse<T>>(url, config).then(res => res.data)
  },

  post: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return userApi.post<ApiResponse<T>>(url, data, config).then(res => res.data)
  },

  put: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return userApi.put<ApiResponse<T>>(url, data, config).then(res => res.data)
  },

  delete: <T = unknown>(
    url: string,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return userApi.delete<ApiResponse<T>>(url, config).then(res => res.data)
  },

  patch: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return userApi.patch<ApiResponse<T>>(url, data, config).then(res => res.data)
  },
}

// 导出默认实例（用户端）
export default userApi
