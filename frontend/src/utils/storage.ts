// 本地存储工具函数
import type { UserInfoVO } from '@/types/user'

const TOKEN_KEY = 'trip_doge_token'
const USER_INFO_KEY = 'trip_doge_user_info'

// Token管理
export const tokenStorage = {
  get: (): string | null => {
    return localStorage.getItem(TOKEN_KEY)
  },
  set: (token: string): void => {
    localStorage.setItem(TOKEN_KEY, token)
  },
  remove: (): void => {
    localStorage.removeItem(TOKEN_KEY)
  },
}

// 用户信息管理
export const userInfoStorage = {
  get: (): UserInfoVO | null => {
    const info = localStorage.getItem(USER_INFO_KEY)
    return info ? JSON.parse(info) : null
  },
  set: (info: UserInfoVO): void => {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
  },
  remove: (): void => {
    localStorage.removeItem(USER_INFO_KEY)
  },
}

// 清除所有存储
export const clearStorage = (): void => {
  tokenStorage.remove()
  userInfoStorage.remove()
}
