// 角色相关类型定义

// 角色信息VO
export interface RoleInfoVO {
  id: number
  code: string
  name: string
  avatarUrl?: string
  description?: string
  roleSetting?: string
  conversationId?: string
}

// 角色详情VO
export interface RoleDetailVO {
  id: number
  code: string
  name: string
  avatarUrl?: string
  description?: string
  personality?: string[]
  specialties?: string[]
  sortOrder?: number
}

// 角色列表查询DTO
export interface RoleListQueryDTO {
  pageNum?: number
  pageSize?: number
}
