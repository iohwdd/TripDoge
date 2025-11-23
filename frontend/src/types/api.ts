// API响应类型定义
export interface ApiResponse<T = unknown> {
  code: number
  data: T
  message: string
}

// 错误码常量
export const ErrorCode = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
} as const

// 请求配置类型
export interface RequestConfig {
  showError?: boolean // 是否显示错误提示
  showLoading?: boolean // 是否显示加载提示
  timeout?: number // 请求超时时间
}
