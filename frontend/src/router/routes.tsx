import type { RouteObject } from 'react-router-dom'
import Auth from '@/pages/user/Auth'
import UserLayout from '@/components/common/UserLayout'

// 用户端路由
export const userRoutes: RouteObject[] = [
  {
    path: '/user',
    element: <UserLayout />,
    children: [
      {
        index: true,
        element: <div>用户端首页</div>,
      },
      {
        path: 'login',
        element: <Auth />,
      },
      {
        path: 'register',
        element: <Auth />,
      },
      {
        path: 'auth',
        element: <Auth />,
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
