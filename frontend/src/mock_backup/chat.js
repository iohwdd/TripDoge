import Mock from 'mockjs'

const history = [
  {
    id: 1,
    role: 'assistant',
    content: '你好！我是小柴，很高兴见到你！我们今天去哪里冒险呢？🐕',
    created_at: '2023-10-01 10:00:00'
  },
  {
    id: 2,
    role: 'user',
    content: '我想去海边玩。',
    created_at: '2023-10-01 10:01:00'
  },
  {
    id: 3,
    role: 'assistant',
    content: '海边真是个好主意！我们可以去冲浪，或者在沙滩上晒太阳。你喜欢哪种呢？🌊',
    created_at: '2023-10-01 10:01:05'
  }
]

export default [
  // 获取历史记录
  Mock.mock(/\/api\/chat\/\d+\/history/, 'post', () => {
    return {
      code: 200,
      message: 'success',
      data: history
    }
  }),

  // 重置对话
  Mock.mock(/\/api\/chat\/\d+\/reset/, 'post', () => {
    return {
      code: 200,
      message: '重置成功',
      data: null
    }
  }),
  
  // 模拟流式对话 (Mock只能返回一次性数据，流式需要在前端模拟)
  // 注意：这里我们暂时用普通 POST 模拟，前端会把它当做 SSE 的结果处理
  // 前端需要写特殊逻辑：如果是 Mock 环境，则不走 SSE，走普通请求并打字机输出
  Mock.mock(/\/api\/chat\/\d+$/, 'post', () => {
    return `好的，我知道了！这真是一个非常有趣的话题。让我们详细聊聊吧。\n\n首先，关于你提到的这一点，我觉得非常有见地。作为一名旅行专家，我建议我们可以...`
  })
]

