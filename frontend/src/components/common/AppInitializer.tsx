// 应用初始化组件
import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { Spin } from 'antd'
import { useUserStore } from '@/stores/userStore'

interface AppInitializerProps {
  children: ReactNode
}

/**
 * 应用初始化组件
 * 在应用启动时检查Token并获取用户信息
 */
const AppInitializer = ({ children }: AppInitializerProps) => {
  const { initialized, loading, initUserInfo } = useUserStore()

  useEffect(() => {
    // 应用启动时初始化用户信息
    if (!initialized) {
      initUserInfo()
    }
  }, [initialized, initUserInfo])

  // 如果还在初始化，显示加载状态
  if (!initialized || loading) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
        }}
      >
        <Spin size="large" tip="正在初始化..." />
      </div>
    )
  }

  return <>{children}</>
}

export default AppInitializer

