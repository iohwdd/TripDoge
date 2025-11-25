# LLM Provider 配置指南

**更新时间**: 2025-11-24  
**重构版本**: refactor/llm-provider

---

## 📋 概述

系统现在支持可插拔的LLM Provider架构，可以通过简单的配置切换不同的LLM提供商，无需修改代码。

### 支持的Provider

1. **Mock** (默认) - 无需API Key，用于开发和测试
2. **DashScope** - 阿里云通义千问
3. **DeepSeek** - DeepSeek AI

---

## 🚀 快速开始

### 1. Mock Provider（默认，无需配置）

系统默认使用Mock Provider，无需任何API Key即可启动：

```bash
# 直接启动，无需配置
mvn spring-boot:run
```

**特点**：
- ✅ 无需API Key
- ✅ 可以正常启动系统
- ⚠️ 实际调用会失败（使用Mock端点）

### 2. DashScope Provider

使用阿里云通义千问：

```bash
# 设置环境变量
export DASHSCOPE_API_KEY="your-dashscope-api-key"
export LLM_PROVIDER="dashscope"

# 启动服务
mvn spring-boot:run
```

**配置文件** (`application.yaml`):
```yaml
llm:
  provider: dashscope

langchain4j:
  community:
    dashscope:
      streaming-chat-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: qwen3-max
        temperature: 0.7
      chat-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: qwen3-max
        temperature: 0.7
```

**特点**：
- ✅ 功能完整（支持RAG、Embedding）
- ✅ 支持MCP工具调用
- ✅ 支持PgVector向量存储

### 3. DeepSeek Provider

使用DeepSeek AI：

```bash
# 设置环境变量
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"
export LLM_PROVIDER="deepseek"

# 启动服务
mvn spring-boot:run
```

**配置文件** (`application.yaml`):
```yaml
llm:
  provider: deepseek

langchain4j:
  open-ai:
    streaming-chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60
    chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60
```

**特点**：
- ✅ 兼容OpenAI格式
- ⚠️ 可能不支持Embedding（使用内存存储）
- ⚠️ MCP功能可能受限

---

## ⚙️ 配置说明

### 核心配置

在 `application.yaml` 中设置：

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}  # mock | dashscope | deepseek
```

### 环境变量

| Provider | 必需环境变量 | 说明 |
|----------|------------|------|
| mock | 无 | 默认Provider，无需配置 |
| dashscope | `DASHSCOPE_API_KEY` | 阿里云通义千问API Key |
| deepseek | `DEEPSEEK_API_KEY` | DeepSeek API Key |

### Provider特定配置

#### DashScope配置

```yaml
langchain4j:
  community:
    dashscope:
      streaming-chat-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: qwen3-max  # 可选: qwen-turbo, qwen-plus, qwen-max
        temperature: 0.7
      chat-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: qwen3-max
        temperature: 0.7
      embedding-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: text-embedding-v3
        dimensions: 1024
```

#### DeepSeek配置

```yaml
langchain4j:
  open-ai:
    streaming-chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60
    chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60
```

---

## 🔄 切换Provider

### 方法1：环境变量（推荐）

```bash
export LLM_PROVIDER="dashscope"
mvn spring-boot:run
```

### 方法2：配置文件

修改 `application.yaml`:
```yaml
llm:
  provider: dashscope  # 或 deepseek, mock
```

### 方法3：启动参数

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=dashscope"
```

---

## 📝 迁移指南

### 从旧配置迁移

**旧方式**（使用profile）:
```bash
export SPRING_PROFILES_ACTIVE="ai"
export DASHSCOPE_API_KEY="your-key"
```

**新方式**（使用llm.provider）:
```bash
export LLM_PROVIDER="dashscope"
export DASHSCOPE_API_KEY="your-key"
```

### 配置文件变更

**旧配置** (`application-ai.yaml`):
```yaml
spring:
  profiles:
    active: ai
```

**新配置** (`application.yaml`):
```yaml
llm:
  provider: dashscope
```

---

## 🎯 最佳实践

1. **开发环境**：使用Mock Provider，无需API Key
2. **测试环境**：使用DashScope Provider，功能完整
3. **生产环境**：根据需求选择合适的Provider

### 推荐配置

**开发环境** (`application-dev.yaml`):
```yaml
llm:
  provider: mock
```

**测试环境** (`application-test.yaml`):
```yaml
llm:
  provider: dashscope
```

**生产环境** (`application-prod.yaml`):
```yaml
llm:
  provider: dashscope  # 或 deepseek
```

---

## ⚠️ 注意事项

1. **Mock Provider限制**：
   - 实际调用会失败（使用Mock端点）
   - 仅用于系统启动和核心功能测试

2. **DeepSeek限制**：
   - 可能不支持Embedding（使用内存存储）
   - MCP功能可能受限

3. **DashScope优势**：
   - 功能完整（RAG、Embedding、MCP）
   - 支持PgVector向量存储

---

## 🔍 故障排查

### 问题1：启动失败，提示找不到Bean

**原因**：未配置LLM Provider或配置错误

**解决**：
```bash
# 确保配置了llm.provider
export LLM_PROVIDER="mock"  # 或 dashscope, deepseek
```

### 问题2：DashScope Provider启动失败

**原因**：缺少API Key

**解决**：
```bash
export DASHSCOPE_API_KEY="your-key"
export LLM_PROVIDER="dashscope"
```

### 问题3：DeepSeek Provider启动失败

**原因**：缺少API Key或配置错误

**解决**：
```bash
export DEEPSEEK_API_KEY="sk-your-key"
export LLM_PROVIDER="deepseek"
```

---

## 📚 相关文档

- [回滚重构方案评估报告](./回滚重构方案评估报告.md)
- [重构进度报告](./重构进度报告.md)

---

**最后更新时间**: 2025-11-24

