// 路由守卫组件 - 保护需要登录的路由
import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useUserStore } from '@/stores'
import { UserRole } from '@/types/user'

interface ProtectedRouteProps {
  children: React.ReactNode
  /**
   * 允许的角色，如果不指定则所有登录用户都可以访问
   */
  allowedRoles?: Array<(typeof UserRole)[keyof typeof UserRole]>
  /**
   * 未登录时跳转的路径，默认为用户登录页
   */
  redirectTo?: string
}

/**
 * 受保护的路由组件
 * - 未登录用户访问时跳转到登录页
 * - 已登录但角色不匹配时显示无权限提示
 */
const ProtectedRoute = ({
  children,
  allowedRoles,
  redirectTo = '/user/login',
}: ProtectedRouteProps) => {
  const location = useLocation()
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

  // 如果未登录，跳转到登录页，并保存当前路径以便登录后跳转回来
  if (!userInfo) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />
  }

  // 如果指定了允许的角色，检查用户角色
  if (allowedRoles && allowedRoles.length > 0) {
    if (!userInfo.role || !allowedRoles.includes(userInfo.role)) {
      // 角色不匹配，显示无权限提示
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
          <p>您没有权限访问此页面</p>
          <a href="/user">返回首页</a>
        </div>
      )
    }
  }

  // 已登录且角色匹配，允许访问
  return <>{children}</>
}

export default ProtectedRoute
