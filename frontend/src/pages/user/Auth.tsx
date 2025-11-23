import { useState, useEffect } from 'react'
import { Form, Input, Button, message, Card, Tabs, Checkbox } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, SafetyOutlined } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'
import { login, register } from '@/api/user/auth'
import { validateEmail, validatePassword, validateNickname, validateCode } from '@/utils/validation'
import { tokenStorage } from '@/utils/storage'
import { useEmailCode } from '@/hooks'
import { useUserStore } from '@/stores'
import type { UserLoginDTO, UserRegisterDTO } from '@/types/user'
import './Auth.css'

const Auth = () => {
  const navigate = useNavigate()
  const location = useLocation()
  // 根据URL查询参数确定初始选项卡
  const getInitialTab = () => {
    const searchParams = new URLSearchParams(location.search)
    return searchParams.get('tab') === 'register' ? 'register' : 'login'
  }
  const [activeTab, setActiveTab] = useState<string>(getInitialTab())

  // 当URL变化时，同步更新选项卡（支持查询参数）
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search)
    const tabParam = searchParams.get('tab')
    if (tabParam === 'register') {
      setActiveTab('register')
    } else {
      setActiveTab('login')
    }
  }, [location.pathname, location.search])
  const [loginForm] = Form.useForm()
  const [registerForm] = Form.useForm()
  const [loginLoading, setLoginLoading] = useState(false)
  const [registerLoading, setRegisterLoading] = useState(false)

  // 使用邮箱验证码Hook
  const { codeLoading, countdown, sendCode } = useEmailCode()

  // 使用用户store
  const { setUserInfo } = useUserStore()

  // 发送验证码
  const handleSendCode = async () => {
    const email = registerForm.getFieldValue('email')
    await sendCode(email)
  }

  // 提交登录
  const handleLogin = async (values: UserLoginDTO & { remember?: boolean }) => {
    try {
      setLoginLoading(true)
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { remember, ...loginData } = values
      const res = await login(loginData)
      if (res.code === 200 && res.data) {
        const { token, userInfo } = res.data
        // 保存Token
        tokenStorage.set(token)
        // 更新用户信息到store（会自动同步到localStorage）
        setUserInfo(userInfo)
        message.success('登录成功')

        // 获取之前尝试访问的页面（如果有）
        const from = (location.state as { from?: { pathname: string } })?.from?.pathname

        // 根据用户角色跳转
        if (userInfo.role === 'ADMIN') {
          // 管理员：如果有之前访问的页面且是管理端路由，跳转到该页面；否则跳转到管理端首页
          navigate(from && from.startsWith('/admin') ? from : '/admin', { replace: true })
        } else {
          // 普通用户：如果有之前访问的页面且是用户端路由，跳转到该页面；否则跳转到用户端首页
          navigate(from && from.startsWith('/user') ? from : '/user', { replace: true })
        }
      }
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      setLoginLoading(false)
    }
  }

  // 提交注册
  const handleRegister = async (values: UserRegisterDTO & { confirmPassword?: string }) => {
    try {
      setRegisterLoading(true)
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, ...registerData } = values
      const res = await register(registerData)
      if (res.code === 200) {
        message.success('注册成功，请登录')
        // 切换到登录选项卡
        setActiveTab('login')
        // 将邮箱填入登录表单
        loginForm.setFieldsValue({ email: registerData.email })
        // 清空注册表单
        registerForm.resetFields()
      }
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      setRegisterLoading(false)
    }
  }

  const tabItems = [
    {
      key: 'login',
      label: '登录',
      children: (
        <Form
          form={loginForm}
          name="login"
          onFinish={handleLogin}
          autoComplete="off"
          layout="vertical"
          size="large"
          initialValues={{
            remember: true,
          }}
        >
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve()
                  if (!validateEmail(value)) {
                    return Promise.reject('请输入正确的邮箱格式')
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="请输入邮箱" autoComplete="email" />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度至少8位' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item>
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <Button
              type="link"
              onClick={() => navigate('/user/forgot-password')}
              style={{ float: 'right' }}
            >
              忘记密码？
            </Button>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loginLoading}>
              登录
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'register',
      label: '注册',
      children: (
        <Form
          form={registerForm}
          name="register"
          onFinish={handleRegister}
          autoComplete="off"
          layout="vertical"
          size="large"
        >
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve()
                  if (!validateEmail(value)) {
                    return Promise.reject('请输入正确的邮箱格式')
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="请输入邮箱" autoComplete="email" />
          </Form.Item>

          <Form.Item
            name="nickname"
            label="昵称"
            rules={[
              { required: true, message: '请输入昵称' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve()
                  const result = validateNickname(value)
                  if (!result.valid) {
                    return Promise.reject(result.message)
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入昵称（2-20位）"
              autoComplete="nickname"
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[
              { required: true, message: '请输入密码' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve()
                  const result = validatePassword(value)
                  if (!result.valid) {
                    return Promise.reject(result.message)
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码（至少8位，包含大小写字母和数字）"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject('两次输入的密码不一致')
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请再次输入密码"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            name="code"
            label="验证码"
            rules={[
              { required: true, message: '请输入验证码' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve()
                  const result = validateCode(value)
                  if (!result.valid) {
                    return Promise.reject(result.message)
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <Input
              prefix={<SafetyOutlined />}
              placeholder="请输入6位验证码"
              maxLength={6}
              suffix={
                <Button
                  type="link"
                  onClick={handleSendCode}
                  disabled={countdown > 0 || codeLoading}
                  loading={codeLoading}
                >
                  {countdown > 0 ? `${countdown}秒后重试` : '发送验证码'}
                </Button>
              }
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={registerLoading}>
              注册
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ]

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <Tabs
          activeKey={activeTab}
          onChange={key => {
            setActiveTab(key)
            // 切换选项卡时更新URL查询参数
            if (key === 'register') {
              navigate('/user/login?tab=register', { replace: true })
            } else {
              navigate('/user/login', { replace: true })
            }
          }}
          items={tabItems}
          centered
          size="large"
        />
      </Card>
    </div>
  )
}

export default Auth
