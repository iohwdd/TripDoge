import { createBrowserRouter, Navigate } from 'react-router-dom'
import { userRoutes, adminRoutes } from './routes'
import NotFound from '@/pages/common/NotFound'

// 路由配置
const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/user/login" replace />,
  },
  // 兼容旧路由路径，重定向到新路径
  {
    path: '/login',
    element: <Navigate to="/user/login" replace />,
  },
  {
    path: '/register',
    element: <Navigate to="/user/register" replace />,
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
