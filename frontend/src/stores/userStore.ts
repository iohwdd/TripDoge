// 用户状态管理store
import { create } from 'zustand'
import { getUserInfo } from '@/api/user/auth'
import { tokenStorage, userInfoStorage, clearStorage } from '@/utils/storage'
import type { UserInfoVO } from '@/types/user'

interface UserState {
  // 用户信息
  userInfo: UserInfoVO | null
  // 是否已初始化（检查过Token）
  initialized: boolean
  // 是否正在加载用户信息
  loading: boolean
  // 设置用户信息
  setUserInfo: (userInfo: UserInfoVO | null) => void
  // 清除用户信息
  clearUserInfo: () => void
  // 初始化用户信息（检查Token并获取用户信息）
  initUserInfo: () => Promise<void>
  // 刷新用户信息
  refreshUserInfo: () => Promise<void>
}

export const useUserStore = create<UserState>(set => ({
  userInfo: null,
  initialized: false,
  loading: false,

  // 设置用户信息
  setUserInfo: (userInfo: UserInfoVO | null) => {
    set({ userInfo })
    if (userInfo) {
      // 同步到localStorage
      userInfoStorage.set(userInfo)
    } else {
      // 清除localStorage
      userInfoStorage.remove()
    }
  },

  // 清除用户信息
  clearUserInfo: () => {
    set({ userInfo: null })
    clearStorage()
  },

  // 初始化用户信息（应用启动时调用）
  initUserInfo: async () => {
    const token = tokenStorage.get()
    
    // 如果没有Token，直接标记为已初始化
    if (!token) {
      set({ initialized: true, userInfo: null })
      return
    }

    // 尝试从localStorage获取用户信息（快速显示）
    const cachedUserInfo = userInfoStorage.get()
    if (cachedUserInfo) {
      set({ userInfo: cachedUserInfo, initialized: true })
    }

    // 尝试从服务器获取最新用户信息
    try {
      set({ loading: true })
      const res = await getUserInfo()
      
      if (res.code === 200 && res.data) {
        // 获取成功，更新用户信息
        set({ 
          userInfo: res.data, 
          initialized: true,
          loading: false 
        })
        // 同步到localStorage
        userInfoStorage.set(res.data)
      } else {
        // 获取失败，清除Token和用户信息
        set({ 
          userInfo: null, 
          initialized: true,
          loading: false 
        })
        clearStorage()
      }
    } catch (error) {
      // 请求失败（可能是Token过期或网络错误）
      console.error('获取用户信息失败:', error)
      set({ 
        userInfo: null, 
        initialized: true,
        loading: false 
      })
      // 清除可能过期的Token
      clearStorage()
    }
  },

  // 刷新用户信息
  refreshUserInfo: async () => {
    const token = tokenStorage.get()
    if (!token) {
      set({ userInfo: null })
      return
    }

    try {
      set({ loading: true })
      const res = await getUserInfo()
      
      if (res.code === 200 && res.data) {
        set({ 
          userInfo: res.data,
          loading: false 
        })
        userInfoStorage.set(res.data)
      } else {
        // 获取失败，清除Token和用户信息
        set({ 
          userInfo: null,
          loading: false 
        })
        clearStorage()
      }
    } catch (error) {
      console.error('刷新用户信息失败:', error)
      set({ 
        userInfo: null,
        loading: false 
      })
      clearStorage()
    }
  },
}))

