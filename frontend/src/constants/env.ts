// 环境变量配置
export const ENV = {
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:7979',
  APP_TITLE: import.meta.env.VITE_APP_TITLE || 'TripDoge',
  APP_DESCRIPTION: import.meta.env.VITE_APP_DESCRIPTION || '基于LangChain4j的多角色AI对话平台',
} as const
