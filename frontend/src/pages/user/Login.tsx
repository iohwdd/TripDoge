import { useState } from 'react'
import { Form, Input, Button, message, Card, Checkbox } from 'antd'
import { MailOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '@/api/user/auth'
import { validateEmail } from '@/utils/validation'
import { tokenStorage, userInfoStorage } from '@/utils/storage'
import type { UserLoginDTO } from '@/types/user'
import './Login.css'

const Login = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)

  // 提交登录
  const handleSubmit = async (values: UserLoginDTO & { remember?: boolean }) => {
    try {
      setLoading(true)
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { remember, ...loginData } = values
      const res = await login(loginData)
      if (res.code === 200 && res.data) {
        const { token, userInfo } = res.data
        // 保存Token和用户信息
        tokenStorage.set(token)
        userInfoStorage.set(userInfo)
        message.success('登录成功')
        // 跳转到用户首页
        navigate('/user')
      }
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <Card className="login-card" title="用户登录">
        <Form
          form={form}
          name="login"
          onFinish={handleSubmit}
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
            <Button type="link" onClick={() => navigate('/user/forgot-password')} style={{ float: 'right' }}>
              忘记密码？
            </Button>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登录
            </Button>
          </Form.Item>

          <Form.Item>
            <div className="login-footer">
              <span>还没有账号？</span>
              <Button type="link" onClick={() => navigate('/user/register')}>
                立即注册
              </Button>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login

