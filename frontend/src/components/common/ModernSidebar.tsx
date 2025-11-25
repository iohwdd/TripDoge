import React, { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Avatar, Tooltip } from 'antd'
import { MessageOutlined, FileTextOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons'
import { useUserStore } from '@/stores'
import { logout } from '@/api/user/auth'
import './ModernSidebar.css'

interface NavItem {
  key: string
  icon: React.ReactNode
  label: string
  path: string
}

const ModernSidebar: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, clearUserInfo } = useUserStore()
  const [activeTab, setActiveTab] = useState<string>('chat')

  // 一级导航菜单（移除角色广场）
  const navItems: NavItem[] = [
    {
      key: 'home',
      icon: <MessageOutlined />,
      label: '对话',
      path: '/user', // 对应首页/对话页
    },
    {
      key: 'documents',
      icon: <FileTextOutlined />,
      label: '知识库',
      path: '/user/documents',
    },
  ]

  // 监听路由变化，更新高亮状态
  useEffect(() => {
    const path = location.pathname
    if (path.includes('/user/documents')) {
      setActiveTab('documents')
    } else if (path.startsWith('/user')) {
      setActiveTab('home')
    }
  }, [location])

  const handleNavClick = (item: NavItem) => {
    setActiveTab(item.key)
    navigate(item.path)
  }

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      clearUserInfo()
      navigate('/user/login')
    }
  }

  return (
    <div className="modern-sidebar">
      {/* Logo 区域 */}
      <div className="sidebar-logo" onClick={() => navigate('/user')}>
        <div className="logo-icon">TD</div>
      </div>

      {/* 导航区域 */}
      <div className="sidebar-nav">
        {navItems.map(item => (
          <Tooltip key={item.key} title={item.label} placement="right">
            <div
              className={`nav-item ${activeTab === item.key ? 'active' : ''}`}
              onClick={() => handleNavClick(item)}
            >
              {item.icon}
            </div>
          </Tooltip>
        ))}
      </div>

      {/* 底部操作区域（移除创建角色按钮） */}
      <div className="sidebar-footer">
        <Tooltip title="个人设置" placement="right">
          <div className="nav-item">
            <SettingOutlined />
          </div>
        </Tooltip>

        <div className="user-avatar-section">
          <Tooltip title={userInfo?.nickname || '用户'} placement="right">
            <Avatar
              src={userInfo?.avatarUrl}
              icon={<UserOutlined />}
              className="user-avatar"
              onClick={() => {
                if (window.confirm('确定要退出登录吗？')) {
                  handleLogout()
                }
              }}
            />
          </Tooltip>
        </div>
      </div>
    </div>
  )
}

export default ModernSidebar
