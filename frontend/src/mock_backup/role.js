import Mock from 'mockjs'

// 模拟三个角色
const roles = [
  {
    id: 1,
    code: 'SHIBA_INU',
    name: '小柴',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Felix',
    // 新增立绘图 (这里暂时用头像代替，实际可以用半身像)
    portraitUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Felix&flip=true', 
    description: '汪汪！我是小柴，一只活泼可爱的柴犬！🐕 天生的冒险家，总是充满激情。今天我们去哪里玩呢？',
    roleSetting: '旅行规划师',
    conversationId: 'conv-shiba-001',
    // 新增氛围配置
    themeColor: '#FF9A2E', // 暖橙
    bgColor: '#FFF8EB',
    bgImage: 'linear-gradient(120deg, #f6d365 0%, #fda085 100%)',
    intimacy: 85, // 亲密度
    statusText: '正在规划下一次旅行...', // 状态气泡
    tags: ['活泼', '吃货', '忠诚']
  },
  {
    id: 2,
    code: 'RAGDOLL_CAT',
    name: '布布',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Bella',
    portraitUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Bella',
    description: '喵～我是布布，一只温柔的布偶猫小姐姐💕 心情不好的时候找我聊天，我会一直陪着你。',
    roleSetting: '治愈系陪伴',
    conversationId: 'conv-ragdoll-001',
    themeColor: '#FF85C0', // 粉色
    bgColor: '#FFF0F6',
    bgImage: 'linear-gradient(120deg, #ff9a9e 0%, #fecfef 99%, #fecfef 100%)',
    intimacy: 92,
    statusText: '刚刚睡醒，想晒太阳~',
    tags: ['温柔', '粘人', '倾听者']
  },
  {
    id: 3,
    code: 'GREY_WOLF',
    name: '阿尔法',
    avatarUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Max',
    portraitUrl: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Max&flip=true',
    description: '嗷呜～我是阿尔法。迷茫了吗？让我为你指明方向。哪怕是漫漫长夜，我也与你同行。',
    roleSetting: '人生导师',
    conversationId: 'conv-wolf-001',
    themeColor: '#5C7CFA', // 蓝色
    bgColor: '#F0F5FF',
    bgImage: 'linear-gradient(120deg, #a1c4fd 0%, #c2e9fb 100%)',
    intimacy: 60,
    statusText: '在思考狼群的未来...',
    tags: ['睿智', '冷静', '可靠']
  }
]

export default [
  // 角色列表
  Mock.mock(/\/api\/roles\/list/, 'post', () => {
    return {
      code: 200,
      message: 'success',
      data: roles
    }
  }),

  // 角色详情
  Mock.mock(/\/api\/roles\/\d+\/detail/, 'post', (options) => {
    const match = options.url.match(/\/api\/roles\/(\d+)\/detail/)
    const id = match ? parseInt(match[1]) : 1
    const role = roles.find(r => r.id === id)

    if (role) {
      return {
        code: 200,
        message: 'success',
        data: role
      }
    } else {
      return {
        code: 10202,
        message: '角色不存在'
      }
    }
  })
]
