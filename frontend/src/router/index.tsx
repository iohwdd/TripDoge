import { createBrowserRouter, Navigate } from 'react-router-dom'
import { userRoutes, adminRoutes } from './routes'
import NotFound from '@/pages/common/NotFound'

// 路由配置
const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/user/login" replace />,
  },
  // 兼容旧路由路径，统一重定向到登录页
  {
    path: '/login',
    element: <Navigate to="/user/login" replace />,
  },
  {
    path: '/register',
    element: <Navigate to="/user/login" replace />,
  },
  {
    path: '/user/register',
    element: <Navigate to="/user/login" replace />,
  },
  {
    path: '/user/auth',
    element: <Navigate to="/user/login" replace />,
  },
  ...userRoutes,
  ...adminRoutes,
  // 404页面
  {
    path: '*',
    element: <NotFound />,
  },
])

export default router
