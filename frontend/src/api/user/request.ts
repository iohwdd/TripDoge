// 用户端API请求封装
import { userApi } from '@/utils/request'
import type { ApiResponse, RequestConfig } from '@/types/api'
import type { AxiosRequestConfig } from 'axios'

// 用户端请求方法
export const userRequest = {
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

