import type { RouteObject } from 'react-router-dom'

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
