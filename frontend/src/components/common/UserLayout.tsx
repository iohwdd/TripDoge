import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Space } from 'antd'
import { UserOutlined, LogoutOutlined, HomeOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { useUserStore } from '@/stores'
import { logout } from '@/api/user/auth'

const { Header, Content } = Layout

// 用户端布局组件
const UserLayout = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, clearUserInfo } = useUserStore()

  // 处理登出
  const handleLogout = async () => {
    try {
      await logout()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 无论API调用成功与否，都清除本地存储
      clearUserInfo()
      navigate('/user/login')
    }
  }

  // 用户下拉菜单
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: '个人中心',
      icon: <UserOutlined />,
      onClick: () => {
        // TODO: 跳转到个人中心页面
        console.log('跳转到个人中心')
      },
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
  ]

  // 导航菜单项
  const menuItems: MenuProps['items'] = [
    {
      key: '/user',
      label: '首页',
      icon: <HomeOutlined />,
      onClick: () => navigate('/user'),
    },
    // TODO: 添加更多菜单项（角色列表、对话、文档管理等）
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          padding: '0 24px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
          <div
            style={{
              fontSize: '20px',
              fontWeight: 'bold',
              color: '#1890ff',
              cursor: 'pointer',
            }}
            onClick={() => navigate('/user')}
          >
            TripDoge
          </div>
          <Menu
            mode="horizontal"
            selectedKeys={[location.pathname]}
            items={menuItems}
            style={{ borderBottom: 'none', flex: 1 }}
          />
        </div>
        {userInfo && (
          <Space>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar
                  src={userInfo.avatarUrl}
                  icon={<UserOutlined />}
                  style={{ backgroundColor: '#87d068' }}
                />
                <span>{userInfo.nickname || userInfo.email}</span>
              </Space>
            </Dropdown>
          </Space>
        )}
      </Header>
      <Content style={{ padding: '24px', background: '#f0f2f5' }}>
        <Outlet />
      </Content>
    </Layout>
  )
}

export default UserLayout
