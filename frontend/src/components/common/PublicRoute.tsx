// 公共路由组件 - 已登录用户访问时跳转到首页
import { Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { useUserStore } from '@/stores'

interface PublicRouteProps {
  children: React.ReactNode
  /**
   * 已登录时跳转的路径
   * 根据用户角色决定：管理员跳转到 /admin，普通用户跳转到 /user
   */
  redirectTo?: string
  /**
   * 是否允许已登录用户继续访问该公共路由
   */
  allowAuthenticated?: boolean
}

/**
 * 公共路由组件（如登录页、注册页）
 * - 已登录用户访问时跳转到对应首页
 * - 未登录用户可以正常访问
 */
const PublicRoute = ({ children, redirectTo, allowAuthenticated = false }: PublicRouteProps) => {
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

  // 如果已登录，跳转到对应首页
  if (userInfo && !allowAuthenticated) {
    // 如果指定了跳转路径，使用指定的路径
    if (redirectTo) {
      return <Navigate to={redirectTo} replace />
    }
    // 否则根据用户角色跳转
    if (userInfo.role === 'ADMIN') {
      return <Navigate to="/admin" replace />
    }
    return <Navigate to="/user" replace />
  }

  // 未登录，允许访问
  return <>{children}</>
}

export default PublicRoute
