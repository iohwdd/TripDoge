// 管理端路由守卫组件 - 仅管理员可访问
import { Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { useUserStore } from '@/stores'
import { UserRole } from '@/types/user'

interface AdminRouteProps {
  children: React.ReactNode
}

/**
 * 管理端路由守卫组件
 * - 未登录用户访问时跳转到登录页
 * - 普通用户访问时显示无权限提示
 * - 仅管理员可以访问
 */
const AdminRoute = ({ children }: AdminRouteProps) => {
  const { userInfo, initialized } = useUserStore()

  // 如果还在初始化，显示加载状态
  if (!initialized) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
        }}
      >
        <Spin size="large" tip="正在检查登录状态..." />
      </div>
    )
  }

  // 如果未登录，跳转到登录页
  if (!userInfo) {
    return <Navigate to="/user/login" replace />
  }

  // 如果不是管理员，显示无权限提示
  if (userInfo.role !== UserRole.ADMIN) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          gap: '16px',
        }}
      >
        <h2>403 - 无权限访问</h2>
        <p>此页面仅管理员可访问</p>
        <a href="/user">返回用户端首页</a>
      </div>
    )
  }

  // 是管理员，允许访问
  return <>{children}</>
}

export default AdminRoute
