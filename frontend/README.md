# TripDoge Frontend

TripDoge 前端项目，基于 React + TypeScript + Vite 构建。

## 技术栈

- **框架**: React 19.2.0
- **语言**: TypeScript 5.9.3
- **构建工具**: Vite 7.2.4
- **代码规范**: ESLint + Prettier

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint

# 自动修复代码格式
npm run lint:fix

# 格式化代码
npm run format

# 检查代码格式
npm run format:check
```

## 项目结构

```
frontend/
├── src/
│   ├── pages/          # 页面组件
│   │   ├── user/       # 用户端页面
│   │   └── admin/      # 管理端页面
│   ├── components/     # 组件
│   │   ├── user/       # 用户端组件
│   │   ├── admin/      # 管理端组件
│   │   └── common/     # 公共组件
│   ├── api/            # API 请求
│   │   ├── user/       # 用户端API
│   │   └── admin/      # 管理端API
│   ├── utils/          # 工具函数
│   ├── types/           # 类型定义
│   └── ...
├── public/              # 静态资源
└── ...
```

## 路径别名

项目配置了路径别名，可以使用 `@/` 来引用 `src/` 目录：

```typescript
import Component from '@/components/common/Component'
import { api } from '@/api/user/auth'
```

## 环境变量

创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 代码规范

项目使用 ESLint 和 Prettier 进行代码规范检查。

- ESLint 配置: `eslint.config.js`
- Prettier 配置: `.prettierrc`
