// 管理端相关类型定义

// LLM配置VO（待后端实现）
export interface LLMConfigVO {
  id: number
  provider: string
  apiKey: string
  modelName: string
  temperature?: number
  maxTokens?: number
  isDefault?: boolean
  status?: number
}

// LLM配置创建DTO
export interface LLMConfigCreateDTO {
  provider: string
  apiKey: string
  modelName: string
  temperature?: number
  maxTokens?: number
  isDefault?: boolean
}

// 管理员用户VO
export interface AdminUserVO {
  id: number
  email: string
  nickname: string
  avatarUrl?: string
  status: number
  createdAt: string
  updatedAt: string
}

// 管理员用户详情VO
export interface AdminUserDetailVO extends AdminUserVO {
  // 可以扩展更多字段，如统计数据等
  conversationCount?: number
  docCount?: number
}

// 系统配置VO
export interface SystemConfigVO {
  fileUploadMaxSize?: number
  fileUploadAllowedTypes?: string[]
  chatMaxTokens?: number
  // 其他配置项...
}

// 系统配置更新DTO
export interface SystemConfigUpdateDTO {
  fileUploadMaxSize?: number
  fileUploadAllowedTypes?: string[]
  chatMaxTokens?: number
  // 其他配置项...
}

// 系统统计数据VO
export interface SystemStatsVO {
  totalUsers: number
  activeUsers: number
  totalConversations: number
  todayConversations: number
  totalDocs: number
  storageUsed: number
  systemHealth: 'healthy' | 'warning' | 'error'
}
