# 设计系统规范文档

## Design Tokens（设计变量）

本项目采用 **Design Tokens** 思想，所有样式值都通过 CSS 变量定义在 `src/styles/variables.css` 中。

### 使用原则

1. **禁止硬编码颜色值**
   - ❌ 错误：`color: #1890ff`
   - ✅ 正确：`color: var(--color-primary)`

2. **禁止硬编码间距值**
   - ❌ 错误：`padding: 16px`
   - ✅ 正确：`padding: var(--spacing-md)`

3. **禁止行内样式**
   - ❌ 错误：`<div style={{ color: '#1890ff' }}>`
   - ✅ 正确：使用 CSS 类和变量

### 变量分类

#### 颜色变量
- `--color-primary`: 主色
- `--color-success`: 成功色
- `--color-warning`: 警告色
- `--color-error`: 错误色
- `--text-color-primary`: 主要文字颜色
- `--text-color-secondary`: 次要文字颜色
- `--bg-color-base`: 基础背景色
- `--link-hover-color`: 链接悬停颜色

#### 间距变量
- `--spacing-xs`: 4px
- `--spacing-sm`: 8px
- `--spacing-md`: 16px
- `--spacing-lg`: 24px
- `--spacing-xl`: 32px

#### 其他变量
- 字体大小：`--font-size-*`
- 圆角：`--border-radius-*`
- 阴影：`--box-shadow-*`
- 过渡动画：`--transition-base`

### 换肤方案

未来如需更换主题，只需修改 `variables.css` 中的变量值，全站样式自动更新。

## 组件架构原则

### 1. Container vs Presentational（容器组件 vs 展示组件）

- **Container 组件**：负责数据获取、状态管理、业务逻辑
  - 位置：`src/pages/`
  - 示例：`Register.tsx`（处理表单提交、API调用）

- **Presentational 组件**：只负责 UI 渲染
  - 位置：`src/components/common/`
  - 使用 Ant Design 等 UI 库

### 2. 组合优于继承（Composition over Inheritance）

优先使用组合模式，避免创建巨大的、带有很多 props 的组件。

### 3. 渐进式抽象

- ✅ 遇到重复使用 3 次以上的 UI 模式，再提取为组件
- ❌ 不要一开始就创建"万能组件"

## 布局系统

使用 Layout 组件包裹页面内容，便于未来调整布局结构。

- `UserLayout`: 用户端布局（当前为简单的 Outlet）
- 未来可扩展为：`SidebarLayout`、`TopNavLayout` 等

## 样式组织

- `styles/variables.css`: Design Tokens 定义
- `styles/common.css`: 全局公共样式
- `styles/index.css`: 样式入口文件
- `pages/*/Component.css`: 页面级样式（使用 Design Tokens）

