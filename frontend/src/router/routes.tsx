import type { RouteObject } from 'react-router-dom'
import Register from '@/pages/user/Register'

// 用户端路由
export const userRoutes: RouteObject[] = [
  {
    path: '/user',
    element: <div>用户端布局</div>,
    children: [
      {
        index: true,
        element: <div>用户端首页</div>,
      },
      {
        path: 'register',
        element: <Register />,
      },
    ],
  },
]

// 管理端路由
export const adminRoutes: RouteObject[] = [
  {
    path: '/admin',
    element: <div>管理端布局</div>,
    children: [
      {
        index: true,
        element: <div>管理端首页</div>,
      },
    ],
  },
]
