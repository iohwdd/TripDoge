// 管理端API请求封装
import { adminApi } from '@/utils/request'
import type { ApiResponse, RequestConfig } from '@/types/api'
import type { AxiosRequestConfig } from 'axios'

// 管理端请求方法
export const adminRequest = {
  get: <T = unknown>(
    url: string,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return adminApi.get<ApiResponse<T>>(url, config).then(res => res.data)
  },

  post: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return adminApi.post<ApiResponse<T>>(url, data, config).then(res => res.data)
  },

  put: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return adminApi.put<ApiResponse<T>>(url, data, config).then(res => res.data)
  },

  delete: <T = unknown>(
    url: string,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return adminApi.delete<ApiResponse<T>>(url, config).then(res => res.data)
  },

  patch: <T = unknown>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig & RequestConfig
  ): Promise<ApiResponse<T>> => {
    return adminApi.patch<ApiResponse<T>>(url, data, config).then(res => res.data)
  },
}

