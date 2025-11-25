import { useState, useEffect, useMemo } from 'react'
import { Card, Input, Spin, Empty, message, Avatar } from 'antd'
import { SearchOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getRoleList } from '@/api/user/role'
import type { RoleInfoVO } from '@/types/role'
import './RoleList.css'

const RoleList = () => {
  const navigate = useNavigate()
  const [roles, setRoles] = useState<RoleInfoVO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')

  // 获取角色列表
  const fetchRoleList = async () => {
    setLoading(true)
    try {
      const response = await getRoleList()
      if (response.code === 200 && response.data) {
        setRoles(response.data)
      } else {
        message.error(response.message || '获取角色列表失败')
      }
    } catch (error) {
      console.error('获取角色列表失败:', error)
      message.error('获取角色列表失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRoleList()
  }, [])

  // 过滤角色列表
  const filteredRoles = useMemo(() => {
    if (!searchKeyword.trim()) {
      return roles
    }
    const keyword = searchKeyword.toLowerCase()
    return roles.filter(
      role =>
        role.name?.toLowerCase().includes(keyword) ||
        role.description?.toLowerCase().includes(keyword) ||
        role.code?.toLowerCase().includes(keyword)
    )
  }, [roles, searchKeyword])

  // 点击角色卡片
  const handleRoleClick = (role: RoleInfoVO) => {
    // TODO: 跳转到对话页面（后续任务4.1会实现）
    // 暂时跳转到角色详情或对话页面
    if (role.conversationId) {
      navigate(`/user/chat/${role.id}?conversationId=${role.conversationId}`)
    } else {
      navigate(`/user/chat/${role.id}`)
    }
  }

  return (
    <div className="role-list-container">
      <div className="role-list-header">
        <h1 className="role-list-title">AI角色列表</h1>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索角色名称、描述或代码..."
          value={searchKeyword}
          onChange={e => setSearchKeyword(e.target.value)}
          allowClear
          size="large"
          className="role-list-search"
        />
      </div>

      <Spin spinning={loading}>
        {filteredRoles.length === 0 && !loading ? (
          <Empty
            description={searchKeyword ? '未找到匹配的角色' : '暂无角色'}
            style={{ marginTop: '60px' }}
          />
        ) : (
          <div className="role-list-grid">
            {filteredRoles.map(role => (
              <Card
                key={role.id}
                hoverable
                className="role-card"
                onClick={() => handleRoleClick(role)}
                cover={
                  <div className="role-card-cover">
                    <Avatar
                      src={role.avatarUrl}
                      icon={<UserOutlined />}
                      size={80}
                      className="role-card-avatar"
                    />
                  </div>
                }
              >
                <Card.Meta
                  title={<div className="role-card-title">{role.name}</div>}
                  description={
                    <div className="role-card-description">{role.description || '暂无描述'}</div>
                  }
                />
              </Card>
            ))}
          </div>
        )}
      </Spin>
    </div>
  )
}

export default RoleList
