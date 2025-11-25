import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Avatar, Input, Button, message, Spin, Empty } from 'antd'
import {
  UserOutlined,
  SendOutlined,
  ReloadOutlined,
  PictureOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import { getRoleDetail } from '@/api/user/role'
import { getChatHistory, resetChat } from '@/api/user/chat'
import type { RoleDetailVO } from '@/types/role'
import type { ChatHistoryVO } from '@/types/chat'
import { useUserStore } from '@/stores'
import './Chat.css'

const Chat = () => {
  const { roleId } = useParams<{ roleId: string }>()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { userInfo } = useUserStore()

  const [roleInfo, setRoleInfo] = useState<RoleDetailVO | null>(null)
  const [messages, setMessages] = useState<ChatHistoryVO[]>([])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [resetting, setResetting] = useState(false)

  const messagesEndRef = useRef<HTMLDivElement>(null)
  const messagesContainerRef = useRef<HTMLDivElement>(null)

  // 获取角色信息
  useEffect(() => {
    if (!roleId) {
      message.error('角色ID不存在')
      navigate('/user')
      return
    }

    const fetchRoleInfo = async () => {
      try {
        setLoading(true)
        const response = await getRoleDetail(Number(roleId))
        if (response.code === 200 && response.data) {
          setRoleInfo(response.data)
        } else {
          message.error('获取角色信息失败')
          navigate('/user')
        }
      } catch (error) {
        console.error('获取角色信息失败:', error)
        message.error('获取角色信息失败')
        navigate('/user')
      } finally {
        setLoading(false)
      }
    }

    fetchRoleInfo()
  }, [roleId, navigate])

  // 获取对话历史
  useEffect(() => {
    if (!roleId) return

    const fetchHistory = async () => {
      try {
        const response = await getChatHistory(Number(roleId))
        if (response.code === 200 && response.data) {
          setMessages(response.data)
        }
      } catch (error) {
        console.error('获取对话历史失败:', error)
      }
    }

    fetchHistory()

    // 检查是否有初始消息
    const initialMessage = searchParams.get('initialMessage')
    if (initialMessage) {
      setInputValue(initialMessage)
    }
  }, [roleId, searchParams])

  // 自动滚动到底部
  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }

  // 发送消息（任务4.2中实现SSE流式）
  const handleSend = async () => {
    if (!inputValue.trim() || sending) return

    const userMessage: ChatHistoryVO = {
      id: Date.now(),
      conversationId: '',
      roleId: Number(roleId),
      messageType: 'user',
      content: inputValue.trim(),
      createTime: new Date().toISOString(),
    }

    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setSending(true)

    // TODO: 任务4.2中实现SSE流式对话
    // 这里先显示一个占位消息
    setTimeout(() => {
      const aiMessage: ChatHistoryVO = {
        id: Date.now() + 1,
        conversationId: '',
        roleId: Number(roleId),
        messageType: 'assistant',
        content: '流式对话功能将在任务4.2中实现...',
        createTime: new Date().toISOString(),
      }
      setMessages(prev => [...prev, aiMessage])
      setSending(false)
    }, 1000)
  }

  // 重置对话
  const handleReset = async () => {
    if (!roleId) return

    try {
      setResetting(true)
      const response = await resetChat(Number(roleId))
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

  // 上传图片（任务4.2中实现）
  const handleUploadImage = () => {
    message.info('图片上传功能将在任务4.2中实现')
  }

  // 处理回车发送
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  if (loading) {
    return (
      <div className="chat-loading">
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (!roleInfo) {
    return (
      <div className="chat-error">
        <Empty description="角色信息不存在" />
      </div>
    )
  }

  return (
    <div className="chat-container">
      {/* 顶部栏 */}
      <div className="chat-header">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/user')}
          className="chat-back-btn"
        >
          返回
        </Button>
        <div className="chat-header-info">
          <Avatar src={roleInfo.avatarUrl} icon={<UserOutlined />} size={40} />
          <div className="chat-header-text">
            <div className="chat-header-name">{roleInfo.name}</div>
            <div className="chat-header-desc">{roleInfo.description || 'AI助手'}</div>
          </div>
        </div>
        <Button
          type="text"
          icon={<ReloadOutlined />}
          onClick={handleReset}
          loading={resetting}
          className="chat-reset-btn"
          title="重置对话"
        >
          重置
        </Button>
      </div>

      {/* 消息列表 */}
      <div className="chat-messages" ref={messagesContainerRef}>
        {messages.length === 0 ? (
          <div className="chat-empty">
            <Empty description="还没有对话记录，开始对话吧！" />
          </div>
        ) : (
          messages.map(msg => (
            <div
              key={msg.id}
              className={`chat-message ${msg.messageType === 'user' ? 'user-message' : 'ai-message'}`}
            >
              <div className="message-avatar">
                {msg.messageType === 'user' ? (
                  <Avatar src={userInfo?.avatarUrl} icon={<UserOutlined />} size={36} />
                ) : (
                  <Avatar src={roleInfo.avatarUrl} icon={<UserOutlined />} size={36} />
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
          ))
        )}
        {sending && (
          <div className="chat-message ai-message">
            <div className="message-avatar">
              <Avatar src={roleInfo.avatarUrl} icon={<UserOutlined />} size={36} />
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
            value={inputValue}
            onChange={e => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入消息..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            className="chat-input"
            disabled={sending}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            loading={sending}
            disabled={!inputValue.trim() || sending}
            className="chat-send-btn"
          >
            发送
          </Button>
        </div>
      </div>
    </div>
  )
}

export default Chat
