import { createBrowserRouter, Navigate } from 'react-router-dom'
import { userRoutes, adminRoutes } from './routes'

// 路由配置
const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/user" replace />,
  },
  ...userRoutes,
  ...adminRoutes,
])

export default router
