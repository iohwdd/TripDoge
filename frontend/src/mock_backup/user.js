import Mock from 'mockjs'

const tokens = {
  admin: 'admin-token',
  editor: 'editor-token'
}

const users = {
  'admin-token': {
    id: 1,
    email: '884217544@qq.com',
    nickname: 'Admin Doge',
    avatarUrl: 'https://p1-arco.byteimg.com/tos-cn-i-uwbnlip3yd/3ee5f13fb09879ecb5185e440cef6eb9.png~tplv-uwbnlip3yd-webp.webp',
    status: 1
  }
}

export default [
  // 登录
  Mock.mock(/\/api\/user\/login/, 'post', (options) => {
    const body = JSON.parse(options.body)
    const { email, password } = body
    
    // 简单模拟验证
    if (email === '884217544@qq.com' && password === '123123') {
      return {
        code: 200,
        message: '登录成功',
        data: {
          token: tokens.admin,
          userInfo: users[tokens.admin]
        }
      }
    } else {
      return {
        code: 10103,
        message: '账号或密码错误'
      }
    }
  }),

  // 注册
  Mock.mock(/\/api\/user\/register/, 'post', () => {
    return {
      code: 200,
      message: '注册成功',
      data: users[tokens.admin]
    }
  }),

  // 发送验证码
  Mock.mock(/\/api\/user\/sendEmail/, 'post', () => {
    return {
      code: 200,
      message: '验证码发送成功',
      data: {
        code: '123123'
      }
    }
  }),

  // 获取用户信息
  Mock.mock(/\/api\/user\/info/, 'post', () => {
    return {
      code: 200,
      message: 'success',
      data: users[tokens.admin]
    }
  }),

  // 登出
  Mock.mock(/\/api\/user\/logout/, 'post', () => {
    return {
      code: 200,
      message: '退出成功',
      data: null
    }
  })
]

