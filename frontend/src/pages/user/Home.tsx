import { useState, useEffect, useMemo, useRef } from 'react'
import {
  Avatar,
  Spin,
  Input,
  message,
  Button,
  Checkbox,
  Segmented,
  Modal,
  Form,
  Select,
} from 'antd'
import {
  UserOutlined,
  SearchOutlined,
  PlusOutlined,
  UsergroupAddOutlined,
  SendOutlined,
  TeamOutlined,
  EditOutlined,
  DownOutlined,
  RightOutlined,
  ReloadOutlined,
  PictureOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getRoleList, getRoleDetail } from '@/api/user/role'
import { getChatHistory, resetChat } from '@/api/user/chat'
import type { RoleInfoVO, RoleDetailVO } from '@/types/role'
import type { ChatHistoryVO } from '@/types/chat'
import { useUserStore } from '@/stores'
import './Home.css'
import './Chat.css'

// 群组类型定义
interface ExpertGroup {
  id: string
  name: string
  description?: string
  expertIds: number[]
}

const Home = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [roles, setRoles] = useState<RoleInfoVO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  // 选中的专家ID集合
  const [selectedExpertIds, setSelectedExpertIds] = useState<number[]>([])
  // 底部输入框内容
  const [commandInput, setCommandInput] = useState('')
  // 选择模式：单聊 vs 群组
  const [selectionMode, setSelectionMode] = useState<'single' | 'group'>('single')
  // 群组列表（预定义 + 自定义）
  const [expertGroups, setExpertGroups] = useState<ExpertGroup[]>([
    {
      id: 'six-thinking-hats',
      name: '六顶思考帽',
      description: '多角度思维分析',
      expertIds: [], // 需要根据实际专家ID填充
    },
    {
      id: 'strategy-analysis',
      name: '策略分析专家群组',
      description: '战略规划与决策支持',
      expertIds: [],
    },
  ])
  // 群组管理弹窗
  const [groupModalVisible, setGroupModalVisible] = useState(false)
  const [editingGroup, setEditingGroup] = useState<ExpertGroup | null>(null)
  // 展开的群组ID集合
  const [expandedGroupIds, setExpandedGroupIds] = useState<Set<string>>(new Set())
  // 专家选择临时状态（用于下拉列表中的选择，点击确定后才真正保存）
  const [tempSelectedExpertIds, setTempSelectedExpertIds] = useState<number[]>([])
  // Select 下拉列表的显示状态
  const [selectOpen, setSelectOpen] = useState(false)
  // 对话相关状态
  const [isChatStarted, setIsChatStarted] = useState(false) // 是否已开始对话
  const [currentRoleId, setCurrentRoleId] = useState<number | null>(null) // 当前对话的角色ID
  const [roleInfo, setRoleInfo] = useState<RoleDetailVO | null>(null) // 当前角色详情
  const [messages, setMessages] = useState<ChatHistoryVO[]>([]) // 消息列表
  const [chatInput, setChatInput] = useState('') // 对话输入框内容
  const [sending, setSending] = useState(false) // 发送中状态
  const [resetting, setResetting] = useState(false) // 重置中状态
  const [loadingRoleInfo, setLoadingRoleInfo] = useState(false) // 加载角色信息中

  const messagesEndRef = useRef<HTMLDivElement>(null)

  // 获取专家列表
  useEffect(() => {
    fetchRoles()
  }, [])

  const fetchRoles = async () => {
    setLoading(true)
    try {
      const response = await getRoleList()
      if (response.code === 200 && response.data) {
        setRoles(response.data)
      }
    } catch (error) {
      console.error('获取专家列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 过滤专家列表
  const filteredRoles = useMemo(() => {
    if (!searchKeyword.trim()) return roles
    return roles.filter(
      role =>
        role.name?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        role.description?.toLowerCase().includes(searchKeyword.toLowerCase())
    )
  }, [roles, searchKeyword])

  // 处理专家选择（单聊模式：多选）
  const toggleExpertSelection = (expertId: number) => {
    setSelectedExpertIds(prev => {
      const newIds = prev.includes(expertId)
        ? prev.filter(id => id !== expertId)
        : [...prev, expertId]

      // 如果选择的专家发生变化，重置对话状态
      if (isChatStarted && (newIds.length === 0 || !newIds.includes(currentRoleId || -1))) {
        setIsChatStarted(false)
        setCurrentRoleId(null)
        setMessages([])
        setChatInput('')
        setRoleInfo(null)
      }

      return newIds
    })
  }

  // 切换群组展开/折叠
  const toggleGroupExpand = (groupId: string, e: React.MouseEvent) => {
    e.stopPropagation()
    setExpandedGroupIds(prev => {
      const newSet = new Set(prev)
      if (newSet.has(groupId)) {
        newSet.delete(groupId)
      } else {
        newSet.add(groupId)
      }
      return newSet
    })
  }

  // 处理群组选择（全选/全不选群组内所有专家）
  const handleGroupSelect = (group: ExpertGroup) => {
    const allSelected = group.expertIds.every(id => selectedExpertIds.includes(id))
    if (allSelected) {
      // 如果全部选中，则全不选
      setSelectedExpertIds(prev => prev.filter(id => !group.expertIds.includes(id)))
    } else {
      // 否则全选（合并现有选中项）
      setSelectedExpertIds(prev => {
        const newSet = new Set([...prev, ...group.expertIds])
        return Array.from(newSet)
      })
    }
  }

  // 处理群组内单个专家的选择
  const toggleExpertInGroup = (expertId: number, e: React.MouseEvent) => {
    e.stopPropagation()
    toggleExpertSelection(expertId)
  }

  // 切换模式时重置选择
  const handleModeChange = (value: 'single' | 'group') => {
    setSelectionMode(value)
    setSelectedExpertIds([])
    // 重置对话状态
    setIsChatStarted(false)
    setCurrentRoleId(null)
    setMessages([])
    setChatInput('')
    setRoleInfo(null)
  }

  // 打开群组管理弹窗
  const openGroupModal = (group?: ExpertGroup) => {
    setEditingGroup(group || null)
    if (group) {
      const expertIds = group.expertIds || []
      form.setFieldsValue({
        name: group.name,
        description: group.description,
        expertIds: expertIds,
      })
      setTempSelectedExpertIds(expertIds)
    } else {
      form.resetFields()
      setTempSelectedExpertIds([])
    }
    setSelectOpen(false) // 重置下拉列表状态
    setGroupModalVisible(true)
  }

  // 确认专家选择（点击确定按钮后）
  const handleConfirmExpertSelection = () => {
    form.setFieldsValue({ expertIds: tempSelectedExpertIds })
    setSelectOpen(false) // 关闭下拉列表
  }

  // 保存群组
  const handleSaveGroup = async () => {
    try {
      const values = await form.validateFields()
      if (editingGroup) {
        // 更新群组
        setExpertGroups(prev =>
          prev.map(g =>
            g.id === editingGroup.id ? { ...g, ...values, expertIds: values.expertIds || [] } : g
          )
        )
        message.success('群组更新成功')
      } else {
        // 创建新群组
        const newGroup: ExpertGroup = {
          id: `group-${Date.now()}`,
          ...values,
          expertIds: values.expertIds || [],
        }
        setExpertGroups(prev => [...prev, newGroup])
        message.success('群组创建成功')
      }
      setGroupModalVisible(false)
      form.resetFields()
    } catch (error) {
      console.error('保存群组失败:', error)
    }
  }

  // 获取角色详情
  const fetchRoleInfo = async (roleId: number) => {
    try {
      setLoadingRoleInfo(true)
      const response = await getRoleDetail(roleId)
      if (response.code === 200 && response.data) {
        setRoleInfo(response.data)
      } else {
        message.error('获取角色信息失败')
      }
    } catch (error) {
      console.error('获取角色信息失败:', error)
      message.error('获取角色信息失败')
    } finally {
      setLoadingRoleInfo(false)
    }
  }

  // 判断是否是角色提示词消息
  const isRolePromptMessage = (content: string): boolean => {
    const promptKeywords = [
      '你是',
      '角色定位',
      '核心技能',
      '表达风格',
      '限定规则',
      '角色设定',
      '系统提示',
    ]
    return promptKeywords.some(keyword => content.includes(keyword))
  }

  // 获取对话历史
  const fetchChatHistory = async (roleId: number) => {
    try {
      const response = await getChatHistory(roleId)
      if (response.code === 200 && response.data) {
        // 过滤掉系统消息和角色提示词消息
        const filteredMessages = response.data.filter(
          msg => msg.messageType !== 'system' && !isRolePromptMessage(msg.content)
        )
        setMessages(filteredMessages)
      }
    } catch (error) {
      console.error('获取对话历史失败:', error)
    }
  }

  // 自动滚动到底部
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  // 开始对话
  const handleStartCommand = async () => {
    if (selectedExpertIds.length === 0) {
      message.warning('请至少选择一位专家')
      return
    }

    const primaryExpertId = selectedExpertIds[0]
    const targetExpert = roles.find(r => r.id === primaryExpertId)
    if (!targetExpert) return

    if (selectedExpertIds.length > 1) {
      message.info('目前仅支持与第一位选中的专家对话，群聊功能开发中...')
    }

    // 保存要发送的消息内容
    const messageToSend = commandInput.trim()

    // 设置当前对话的角色ID
    setCurrentRoleId(primaryExpertId)
    setIsChatStarted(true)

    // 清空底部输入框
    setCommandInput('')

    // 并行获取角色详情和对话历史，不等待完成
    fetchRoleInfo(primaryExpertId)
    fetchChatHistory(primaryExpertId)

    // 如果有初始消息，立即发送（不等待角色信息加载）
    if (messageToSend) {
      // 立即发送消息，不等待角色信息加载
      handleSendMessage(messageToSend)
    }
  }

  // 发送消息
  const handleSendMessage = async (content?: string) => {
    const messageContent = content || chatInput.trim()
    if (!messageContent || sending) {
      // 如果没有currentRoleId，说明还没开始对话，先开始对话
      if (!currentRoleId && selectedExpertIds.length > 0) {
        const primaryExpertId = selectedExpertIds[0]
        setCurrentRoleId(primaryExpertId)
        setIsChatStarted(true)
        setCommandInput('')
        fetchRoleInfo(primaryExpertId)
        fetchChatHistory(primaryExpertId)
        // 立即发送消息
        setTimeout(() => {
          handleSendMessage(messageContent)
        }, 0)
        return
      }
      return
    }

    // 确保有角色ID
    const roleId = currentRoleId || selectedExpertIds[0]
    if (!roleId) return

    const userMessage: ChatHistoryVO = {
      id: Date.now(),
      conversationId: '',
      roleId: roleId,
      messageType: 'user',
      content: messageContent,
      createTime: new Date().toISOString(),
    }

    setMessages(prev => [...prev, userMessage])
    setChatInput('')
    setSending(true)

    // TODO: 任务4.2中实现SSE流式对话
    // 这里先显示一个占位消息
    setTimeout(() => {
      const aiMessage: ChatHistoryVO = {
        id: Date.now() + 1,
        conversationId: '',
        roleId: roleId,
        messageType: 'assistant',
        content: '流式对话功能将在任务4.2中实现...',
        createTime: new Date().toISOString(),
      }
      setMessages(prev => [...prev, aiMessage])
      setSending(false)
    }, 1000)
  }

  // 重置对话
  const handleResetChat = async () => {
    if (!currentRoleId) return

    try {
      setResetting(true)
      const response = await resetChat(currentRoleId)
      if (response.code === 200) {
        setMessages([])
        message.success('对话已重置')
      } else {
        message.error('重置对话失败')
      }
    } catch (error) {
      console.error('重置对话失败:', error)
      message.error('重置对话失败')
    } finally {
      setResetting(false)
    }
  }

  // 处理回车发送
  const handleChatKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  // 上传图片（任务4.2中实现）
  const handleUploadImage = () => {
    message.info('图片上传功能将在任务4.2中实现')
  }

  const handleAddExpert = () => {
    navigate('/user/roles/create')
  }

  const { userInfo } = useUserStore()

  return (
    <div className="home-layout">
      {/* 中间：对话/指令区域 */}
      <div className="home-main-area">
        {isChatStarted && currentRoleId ? (
          // 对话界面
          <div className="home-chat-container">
            {/* 顶部栏 */}
            <div className="chat-header">
              <div className="chat-header-info">
                {roleInfo ? (
                  <>
                    <Avatar src={roleInfo.avatarUrl} icon={<UserOutlined />} size={40} />
                    <div className="chat-header-text">
                      <div className="chat-header-name">{roleInfo.name}</div>
                    </div>
                  </>
                ) : (
                  <>
                    <Avatar icon={<UserOutlined />} size={40} />
                    <div className="chat-header-text">
                      <div className="chat-header-name">
                        {roles.find(r => r.id === currentRoleId)?.name || 'AI助手'}
                      </div>
                    </div>
                  </>
                )}
              </div>
              <Button
                type="text"
                icon={<ReloadOutlined />}
                onClick={handleResetChat}
                loading={resetting}
                className="chat-reset-btn"
                title="重置对话"
              >
                重置
              </Button>
            </div>

            {/* 消息列表 */}
            <div className="chat-messages">
              {loadingRoleInfo && messages.length === 0 ? (
                <div className="chat-loading">
                  <Spin size="large" tip="加载中..." />
                </div>
              ) : (
                <>
                  {messages
                    .filter(
                      msg => msg.messageType !== 'system' && !isRolePromptMessage(msg.content)
                    )
                    .map(msg => (
                      <div
                        key={msg.id}
                        className={`chat-message ${msg.messageType === 'user' ? 'user-message' : 'ai-message'}`}
                      >
                        <div className="message-avatar">
                          {msg.messageType === 'user' ? (
                            <Avatar src={userInfo?.avatarUrl} icon={<UserOutlined />} size={36} />
                          ) : (
                            <Avatar src={roleInfo?.avatarUrl} icon={<UserOutlined />} size={36} />
                          )}
                        </div>
                        <div className="message-content">
                          <div className="message-text">{msg.content}</div>
                          {msg.createTime && (
                            <div className="message-time">
                              {new Date(msg.createTime).toLocaleTimeString('zh-CN', {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  {sending && (
                    <div className="chat-message ai-message">
                      <div className="message-avatar">
                        <Avatar src={roleInfo?.avatarUrl} icon={<UserOutlined />} size={36} />
                      </div>
                      <div className="message-content">
                        <div className="message-text typing-indicator">
                          <span></span>
                          <span></span>
                          <span></span>
                        </div>
                      </div>
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>

            {/* 输入框 */}
            <div className="chat-input-area">
              <div className="chat-input-wrapper">
                <Button
                  type="text"
                  icon={<PictureOutlined />}
                  onClick={handleUploadImage}
                  className="chat-upload-btn"
                  title="上传图片"
                />
                <Input.TextArea
                  value={chatInput}
                  onChange={e => setChatInput(e.target.value)}
                  onKeyDown={handleChatKeyDown}
                  placeholder="输入消息..."
                  autoSize={{ minRows: 1, maxRows: 4 }}
                  className="chat-input"
                  disabled={sending}
                />
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  onClick={() => handleSendMessage()}
                  loading={sending}
                  disabled={!chatInput.trim() || sending}
                  className="chat-send-btn"
                >
                  发送
                </Button>
              </div>
            </div>
          </div>
        ) : (
          // 准备就绪占位符
          <>
            <div className="chat-placeholder">
              <div className="placeholder-content">
                <div className="placeholder-icon">
                  <UsergroupAddOutlined />
                </div>
                <h2>{selectedExpertIds.length > 0 ? `准备就绪` : '会议室'}</h2>
                <p>
                  {selectedExpertIds.length > 0
                    ? `已选中 ${selectedExpertIds.length} 位专家，输入指令开始工作`
                    : '从右侧邀请专家进入会议室'}
                </p>
              </div>
            </div>

            {/* 底部输入框 */}
            <div className="home-command-bar">
              <div className="command-input-wrapper">
                <Input
                  value={commandInput}
                  onChange={e => setCommandInput(e.target.value)}
                  placeholder={
                    selectedExpertIds.length > 0
                      ? `发送给 ${selectedExpertIds.length} 位专家...`
                      : '请先在右侧选择专家...'
                  }
                  size="large"
                  onPressEnter={e => {
                    if (selectedExpertIds.length > 0 && commandInput.trim()) {
                      e.preventDefault()
                      handleStartCommand()
                    }
                  }}
                  suffix={
                    <Button
                      type="primary"
                      shape="circle"
                      icon={<SendOutlined />}
                      onClick={handleStartCommand}
                      disabled={selectedExpertIds.length === 0 || !commandInput.trim()}
                    />
                  }
                  bordered={false}
                  className="command-input"
                />
              </div>
            </div>
          </>
        )}
      </div>

      {/* 右侧：专家选择列表 */}
      <div className="home-right-sidebar">
        <div className="sidebar-header">
          <div className="sidebar-title">专家库</div>
          <Segmented
            value={selectionMode}
            onChange={handleModeChange}
            options={[
              { label: '单聊', value: 'single', icon: <UserOutlined /> },
              { label: '群组', value: 'group', icon: <TeamOutlined /> },
            ]}
            block
            className="mode-switcher"
          />
          <Input
            prefix={<SearchOutlined className="text-gray-400" />}
            placeholder="搜索专家..."
            value={searchKeyword}
            onChange={e => setSearchKeyword(e.target.value)}
            allowClear
            className="sidebar-search"
          />
        </div>

        <div className="sidebar-content">
          <Spin spinning={loading}>
            {selectionMode === 'single' ? (
              // 单聊模式：显示专家列表（多选）
              <div className="role-list">
                {filteredRoles.map(role => {
                  const isSelected = selectedExpertIds.includes(role.id)
                  return (
                    <div
                      key={role.id}
                      className={`sidebar-role-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => toggleExpertSelection(role.id)}
                    >
                      <Checkbox
                        checked={isSelected}
                        className="role-item-checkbox"
                        onClick={e => e.stopPropagation()}
                        onChange={() => toggleExpertSelection(role.id)}
                      />
                      <div className="role-item-avatar">
                        <Avatar src={role.avatarUrl} icon={<UserOutlined />} size={36} />
                        {role.conversationId && <div className="status-dot" />}
                      </div>
                      <div className="role-item-info">
                        <div className="role-item-name">{role.name}</div>
                        <div className="role-item-desc">{role.description || '暂无描述'}</div>
                      </div>
                    </div>
                  )
                })}

                {/* 添加专家按钮 */}
                <div className="sidebar-add-btn" onClick={handleAddExpert}>
                  <PlusOutlined /> 添加专家
                </div>
              </div>
            ) : (
              // 群组模式：显示群组列表
              <div className="group-list">
                {expertGroups.map(group => {
                  const isExpanded = expandedGroupIds.has(group.id)
                  // 计算群组复选框状态：全选、部分选中、未选中
                  const selectedCount = group.expertIds.filter(id =>
                    selectedExpertIds.includes(id)
                  ).length
                  const isAllSelected =
                    group.expertIds.length > 0 && selectedCount === group.expertIds.length
                  const isIndeterminate =
                    selectedCount > 0 && selectedCount < group.expertIds.length
                  // 获取群组内的专家列表
                  const groupExperts = roles.filter(r => group.expertIds.includes(r.id))

                  return (
                    <div key={group.id} className="group-item-wrapper">
                      <div
                        className={`sidebar-group-item ${isAllSelected ? 'selected' : ''}`}
                        onClick={() => handleGroupSelect(group)}
                      >
                        <Checkbox
                          checked={isAllSelected}
                          indeterminate={isIndeterminate}
                          className="group-checkbox"
                          onClick={e => e.stopPropagation()}
                          onChange={() => handleGroupSelect(group)}
                        />
                        <div
                          className="group-expand-icon"
                          onClick={e => toggleGroupExpand(group.id, e)}
                        >
                          {isExpanded ? <DownOutlined /> : <RightOutlined />}
                        </div>
                        <div className="group-item-icon">
                          <TeamOutlined />
                        </div>
                        <div className="group-item-info">
                          <div className="group-item-name">{group.name}</div>
                          <div className="group-item-desc">
                            {group.description || `${group.expertIds.length} 位专家`}
                          </div>
                        </div>
                        <Button
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          onClick={e => {
                            e.stopPropagation()
                            openGroupModal(group)
                          }}
                          className="group-edit-btn"
                        />
                      </div>

                      {/* 展开显示群组内的专家 */}
                      {isExpanded && (
                        <div className="group-experts-list">
                          {groupExperts.length > 0 ? (
                            groupExperts.map(expert => {
                              const isExpertSelected = selectedExpertIds.includes(expert.id)
                              return (
                                <div
                                  key={expert.id}
                                  className={`group-expert-item ${isExpertSelected ? 'selected' : ''}`}
                                  onClick={e => toggleExpertInGroup(expert.id, e)}
                                >
                                  <Checkbox
                                    checked={isExpertSelected}
                                    className="group-expert-checkbox"
                                    onClick={e => e.stopPropagation()}
                                    onChange={() => toggleExpertSelection(expert.id)}
                                  />
                                  <div className="group-expert-avatar">
                                    <Avatar
                                      src={expert.avatarUrl}
                                      icon={<UserOutlined />}
                                      size={28}
                                    />
                                    {expert.conversationId && <div className="status-dot-small" />}
                                  </div>
                                  <div className="group-expert-info">
                                    <div className="group-expert-name">{expert.name}</div>
                                  </div>
                                </div>
                              )
                            })
                          ) : (
                            <div className="group-empty-tip">群组中暂无专家</div>
                          )}
                        </div>
                      )}
                    </div>
                  )
                })}

                {/* 创建群组按钮 */}
                <div className="sidebar-add-btn" onClick={() => openGroupModal()}>
                  <PlusOutlined /> 创建群组
                </div>
              </div>
            )}
          </Spin>
        </div>
      </div>

      {/* 群组管理弹窗 */}
      <Modal
        title={editingGroup ? '编辑群组' : '创建群组'}
        open={groupModalVisible}
        onOk={handleSaveGroup}
        onCancel={() => {
          setGroupModalVisible(false)
          form.resetFields()
        }}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="群组名称"
            rules={[{ required: true, message: '请输入群组名称' }]}
          >
            <Input placeholder="例如：六顶思考帽" />
          </Form.Item>
          <Form.Item name="description" label="群组描述">
            <Input.TextArea placeholder="描述这个群组的用途..." rows={3} />
          </Form.Item>
          <Form.Item
            name="expertIds"
            label="选择专家"
            rules={[{ required: true, message: '请至少选择一位专家' }]}
          >
            <Select
              mode="multiple"
              placeholder="选择群组中的专家"
              value={tempSelectedExpertIds}
              onChange={setTempSelectedExpertIds}
              open={selectOpen}
              onDropdownVisibleChange={setSelectOpen}
              options={roles.map(r => ({
                label: r.name,
                value: r.id,
              }))}
              dropdownRender={menu => (
                <>
                  {menu}
                  <div className="expert-select-footer">
                    <Button
                      type="primary"
                      block
                      onClick={handleConfirmExpertSelection}
                      disabled={tempSelectedExpertIds.length === 0}
                    >
                      确定
                    </Button>
                  </div>
                </>
              )}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Home
