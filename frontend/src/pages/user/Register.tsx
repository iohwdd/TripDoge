import { useState } from 'react'
import { Form, Input, Button, message, Card } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, SafetyOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { register, sendEmailCode } from '@/api/user/auth'
import { validateEmail, validatePassword, validateNickname, validateCode } from '@/utils/validation'
import type { UserRegisterDTO } from '@/types/user'
import './Register.css'

const Register = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [codeLoading, setCodeLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)

  // 发送验证码
  const handleSendCode = async () => {
    const email = form.getFieldValue('email')
    if (!email) {
      message.warning('请先输入邮箱')
      return
    }

    if (!validateEmail(email)) {
      message.error('请输入正确的邮箱格式')
      return
    }

    try {
      setCodeLoading(true)
      const res = await sendEmailCode({ email })
      if (res.code === 200) {
        message.success('验证码已发送，请查收邮箱')
        // 开发模式下显示验证码
        if (import.meta.env.DEV && res.data?.code) {
          message.info(`开发模式验证码: ${res.data.code}`)
        }
        // 开始倒计时
        setCountdown(60)
        const timer = setInterval(() => {
          setCountdown(prev => {
            if (prev <= 1) {
              clearInterval(timer)
              return 0
            }
            return prev - 1
          })
        }, 1000)
      }
    } catch (error) {
      console.error('发送验证码失败:', error)
    } finally {
      setCodeLoading(false)
    }
  }

  // 提交注册
  const handleSubmit = async (values: UserRegisterDTO & { confirmPassword?: string }) => {
    try {
      setLoading(true)
      // 移除confirmPassword字段，只发送注册所需的数据
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, ...registerData } = values
      const res = await register(registerData)
      if (res.code === 200) {
        message.success('注册成功，请登录')
        navigate('/user/login')
      }
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="register-page">
      <Card className="register-card" title="用户注册">
        <Form
          form={form}
          name="register"
          onFinish={handleSubmit}
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
            <Button type="primary" htmlType="submit" block loading={loading}>
              注册
            </Button>
          </Form.Item>

          <Form.Item>
            <div className="register-footer">
              <span>已有账号？</span>
              <Button type="link" onClick={() => navigate('/user/login')}>
                立即登录
              </Button>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Register
