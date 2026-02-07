# TripDoge 前端开发计划 (Arco Design + Mock版)

这是一个基于 Vue 3 + Vite 的 AI 角色扮演对话平台前端开发计划。
UI 框架采用字节跳动 Arco Design Vue。
**特别说明**：当前阶段所有接口请求通过 Mock 数据模拟，不依赖真实后端服务。

## 📦 第一阶段：项目初始化与基础架构

- [ ] **依赖安装与配置**
  - [ ] 安装核心依赖：`vue-router`, `pinia`, `axios`, `@arco-design/web-vue`
  - [ ] 安装工具依赖：`sass`, `markdown-it` (或 `v-md-editor`), `@vueuse/core`
  - [ ] 安装 Mock 依赖：`mockjs`, `vite-plugin-mock` (可选，或手动拦截)
  - [ ] 配置 `vite.config.js`: 设置 `@` 别名
- [ ] **目录结构搭建**
  - [ ] `src/api`: API 接口定义
  - [ ] `src/mock`: Mock 数据定义 (核心)
  - [ ] `src/assets`: 静态资源
  - [ ] `src/components`: 公共组件
  - [ ] `src/layout`: 布局组件
  - [ ] `src/router`: 路由配置
  - [ ] `src/stores`: 状态管理
  - [ ] `src/utils`: 工具函数 (request, auth等)
  - [ ] `src/views`: 页面视图
- [ ] **全局配置**
  - [ ] 引入 Arco Design 样式
  - [ ] 封装 Axios：
    - [ ] 配置 Mock 适配器或拦截器，拦截 API 请求并返回 Mock 数据
    - [ ] 模拟网络延迟 (Delay) 以模拟真实体验
  - [ ] 配置 Vue Router：定义基础路由

## 🔐 第二阶段：用户认证模块 (Mock)

- [ ] **Mock 数据准备**
  - [ ] `POST /api/user/login`: 模拟登录成功，返回 Token 和用户信息
  - [ ] `POST /api/user/register`: 模拟注册成功
  - [ ] `POST /api/user/sendEmail`: 模拟验证码发送成功
- [ ] **注册页面 (`/register`)**
  - [ ] UI 布局 (Arco Design)：邮箱、验证码、密码、确认密码、昵称
  - [ ] 功能：表单校验
  - [ ] 功能：发送验证码 (模拟)
  - [ ] 功能：注册跳转
- [ ] **登录页面 (`/login`)**
  - [ ] UI 布局 (Arco Design)：邮箱、密码
  - [ ] 功能：登录跳转
  - [ ] 状态管理：Pinia 存储 Mock 的用户信息

## 🎭 第三阶段：角色与主页模块 (Mock)

- [ ] **Mock 数据准备**
  - [ ] `POST /api/roles/list`: 返回预设的 3 个角色 (小柴、布布、阿尔法) 数据
- [ ] **主布局 (`MainLayout`)**
  - [ ] Arco Layout 组件：侧边栏导航、顶部栏
  - [ ] 侧边栏：角色列表、知识库入口
- [ ] **角色列表页 (`/roles`)** (首页)
  - [ ] API 对接：调用 Mock 的角色列表接口
  - [ ] UI 实现：Arco Card 组件展示角色信息
  - [ ] 功能：点击进入聊天

## 💬 第四阶段：AI 对话核心模块 (Mock)

- [ ] **Mock 数据准备**
  - [ ] `POST /api/chat/{roleId}/history`: 返回模拟的历史对话记录
  - [ ] `POST /api/chat/{roleId}` (SSE模拟): 
    - [ ] 由于 Mock.js 不支持 SSE，需手动模拟流式输出效果，或使用 `setTimeout` 分段返回数据
- [ ] **聊天布局 (`/chat/:roleId`)**
  - [ ] 顶部：角色信息
  - [ ] 中间：消息列表 (Arco List 或 自定义)
  - [ ] 底部：输入框 (Arco Input/Textarea)、发送按钮
- [ ] **消息渲染**
  - [ ] 支持 Markdown 渲染
  - [ ] 区分用户 (右) 和 AI (左) 样式
- [ ] **流式对话模拟**
  - [ ] 前端模拟 "打字机" 效果：接收到 Mock 的完整回复后，在前端逐步显示
- [ ] **功能操作**
  - [ ] 重置对话 (Mock 接口 `/api/chat/{roleId}/reset`)

## 📚 第五阶段：知识库模块 (Mock)

- [ ] **Mock 数据准备**
  - [ ] `POST /api/doc/list`: 返回模拟文档列表
  - [ ] `POST /api/doc/parse`: 模拟上传成功
  - [ ] `POST /api/doc/delete`: 模拟删除成功
- [ ] **知识库页面 (`/knowledge`)**
  - [ ] UI 实现：Arco Table 展示文档列表
  - [ ] 上传组件：Arco Upload (配置 mock action)
- [ ] **文档操作**
  - [ ] 删除功能 (模拟)

## 🎨 第六阶段：优化与完善

- [ ] **UI/UX 优化**
  - [ ] 适配 TripDoge 风格 (Arco Design 主题配置)
  - [ ] 移动端适配
- [ ] **Mock 开关**
  - [ ] 在 `.env` 中配置 `VITE_USE_MOCK=true`，便于未来切换真实接口

