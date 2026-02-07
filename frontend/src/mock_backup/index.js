import Mock from 'mockjs'

// 设置延时，模拟真实请求
Mock.setup({
  timeout: '200-600'
})

console.log('MockJs is running...')

// 导入其他 mock 模块
import './user'
import './role'
import './chat'
import './doc'

export default Mock

