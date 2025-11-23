// 用户相关类型定义

// 用户角色常量
export const UserRole = {
  USER: 'USER',
  ADMIN: 'ADMIN',
} as const

export type UserRoleType = (typeof UserRole)[keyof typeof UserRole]

// 用户信息VO
export interface UserInfoVO {
  id: number
  email: string
  nickname: string
  avatarUrl?: string
  role?: UserRoleType
}

// 用户登录DTO
export interface UserLoginDTO {
  email: string
  password: string
}

// 用户注册DTO
export interface UserRegisterDTO {
  email: string
  password: string
  nickname: string
  code: string
}

// 登录响应数据
export interface LoginResponse {
  userInfo: UserInfoVO
  token: string
  tokenType: string
}

// 邮箱验证码DTO
export interface EmailCodeDTO {
  email: string
}

// 邮箱验证码VO
export interface EmailCodeVO {
  code: string
}
