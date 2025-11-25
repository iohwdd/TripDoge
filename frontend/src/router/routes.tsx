import type { RouteObject } from 'react-router-dom'
import Auth from '@/pages/user/Auth'
import { RoleList, Home, Chat } from '@/pages/user'
import UserLayout from '@/components/common/UserLayout'
import ProtectedRoute from '@/components/common/ProtectedRoute'
import PublicRoute from '@/components/common/PublicRoute'
import AdminRoute from '@/components/common/AdminRoute'

// 用户端路由
export const userRoutes: RouteObject[] = [
  // 统一的登录/注册页面（使用选项卡切换）
  {
    path: '/user/login',
    element: (
      <PublicRoute>
        <Auth />
      </PublicRoute>
    ),
  },
  // 需要登录的用户端路由
  {
    path: '/user',
    element: (
      <ProtectedRoute>
        <UserLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'roles',
        element: <RoleList />,
      },
      {
        path: 'documents',
        element: <div>文档管理页面（待实现）</div>,
      },
      {
        path: 'chats',
        element: <div>对话历史页面（待实现）</div>,
      },
      {
        path: 'chat/:roleId',
        element: <Chat />,
      },
    ],
  },
]

// 管理端路由
export const adminRoutes: RouteObject[] = [
  {
    path: '/admin',
    element: (
      <AdminRoute>
        <div>管理端布局</div>
      </AdminRoute>
    ),
    children: [
      {
        index: true,
        element: <div>管理端首页</div>,
      },
    ],
  },
]
