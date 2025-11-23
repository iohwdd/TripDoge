// 用户认证相关API
import { userRequest } from './request'
import type { UserRegisterDTO, UserLoginDTO, UserInfoVO, LoginResponse, EmailCodeDTO, EmailCodeVO } from '@/types/user'

/**
 * 用户注册
 */
export const register = (data: UserRegisterDTO) => {
  return userRequest.post<UserInfoVO>('/api/user/register', data)
}

/**
 * 用户登录
 */
export const login = (data: UserLoginDTO) => {
  return userRequest.post<LoginResponse>('/api/user/login', data)
}

/**
 * 发送邮箱验证码
 */
export const sendEmailCode = (data: EmailCodeDTO) => {
  return userRequest.post<EmailCodeVO>('/api/user/sendEmail', data)
}

/**
 * 用户登出
 */
export const logout = () => {
  return userRequest.post('/api/user/logout')
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  return userRequest.post<UserInfoVO>('/api/user/info')
}

