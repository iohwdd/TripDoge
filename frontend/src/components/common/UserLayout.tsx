import { Outlet } from 'react-router-dom'
import ModernSidebar from './ModernSidebar'

// 用户端布局组件 - 重构为三栏式沉浸布局
const UserLayout = () => {
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', backgroundColor: '#f2f3f5' }}>
      {/* 左侧一级导航 (Command Rail) */}
      <ModernSidebar />

      {/* 右侧主工作区 (Main Stage) */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* 这里未来可以放置二级导航栏 (Context Sidebar) 的占位，目前先直接放 Outlet */}
        {/* 如果有二级侧边栏，可以在这里通过路由或状态控制显示 */}
        
        <main style={{ flex: 1, overflow: 'hidden', position: 'relative', minHeight: 0 }}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default UserLayout
