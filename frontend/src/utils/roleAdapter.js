/**
 * 角色数据适配器
 * 用于将后端返回的原始角色数据（包含 JSON 字符串）转换为前端 UI 所需的结构化对象
 */
export function adaptRoleData(rawRole) {
  if (!rawRole) return null

  let setting = {}
  try {
    // 解析 roleSetting JSON 字符串
    if (typeof rawRole.roleSetting === 'string') {
      setting = JSON.parse(rawRole.roleSetting)
    } else if (typeof rawRole.roleSetting === 'object') {
      setting = rawRole.roleSetting
    }
  } catch (e) {
    console.error('解析角色配置失败:', e)
  }

  // 提取视觉配置，如果不存在则使用默认值
  const visual = setting.visual || {}
  const speech = setting.speech_style || {}

  // 决定立绘图：如果有配置则用配置，否则用头像，或者根据 code 映射本地/在线资源
  // 这里为了演示效果，如果 avatarUrl 是默认图，我们可以临时用 dicebear 随机图增强体验
  // 实际项目中应使用 visual.portrait_url 或 rawRole.avatarUrl
  const portraitUrl = visual.portrait_url || rawRole.avatarUrl

  return {
    ...rawRole,
    // 展开 setting 中的字段方便使用
    roleSettingObj: setting,
    
    // 核心视觉字段
    themeColor: visual.theme_color || '#FF9A2E', // 默认暖橙
    bgColor: visual.bg_color || '#FFF8EB',
    bgImage: visual.bg_image || 'linear-gradient(120deg, #f6d365 0%, #fda085 100%)',
    
    // 文本字段
    statusText: visual.status_text || speech.catchphrase || '今天心情不错~',
    roleSettingLabel: rawRole.roleSettingLabel || (setting.expertise ? setting.expertise[0] : 'AI 伙伴'), // 身份标签
    
    // 列表展示用的标签
    tags: setting.personality || [],
    
    // 修正后的图片
    portraitUrl: portraitUrl,
    
    // 亲密度 (如果后端没返回，暂时 mock 一个)
    intimacy: rawRole.intimacy || 0
  }
}

