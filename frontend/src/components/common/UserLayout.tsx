import { Outlet } from 'react-router-dom'
import ModernSidebar from './ModernSidebar'
import './UserLayout.css'

// 用户端布局组件 - 重构为三栏式沉浸布局，支持响应式
const UserLayout = () => {
  return (
    <div className="user-layout-container">
      {/* 左侧一级导航 (Command Rail) - 移动端自动变为底部导航 */}
      <ModernSidebar />

      {/* 右侧主工作区 (Main Stage) */}
      <div className="user-layout-main">
        {/* 这里未来可以放置二级导航栏 (Context Sidebar) 的占位，目前先直接放 Outlet */}
        {/* 如果有二级侧边栏，可以在这里通过路由或状态控制显示 */}

        <main className="user-layout-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default UserLayout
