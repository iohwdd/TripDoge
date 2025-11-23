import { useNavigate } from 'react-router-dom'
import { Button, Result } from 'antd'
import './NotFound.css'

const NotFound = () => {
  const navigate = useNavigate()

  return (
    <div className="not-found-page">
      <Result
        status="404"
        title="404"
        subTitle="抱歉，您访问的页面不存在"
        extra={
          <Button type="primary" onClick={() => navigate('/user/login')}>
            返回登录页
          </Button>
        }
      />
    </div>
  )
}

export default NotFound
