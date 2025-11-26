# API密钥获取和配置指南

## 一、DashScope API Key（阿里云通义千问）

### 如何获取 DashScope API Key

1. **访问阿里云 DashScope 控制台**
   - 网址：https://dashscope.console.aliyun.com/
   - 需要阿里云账号

2. **注册并登录**
   - 如果没有阿里云账号，需要先注册
   - 登录后进入 DashScope 控制台

3. **创建 API Key**
   - 在控制台中找到"API密钥管理"
   - 点击"创建API密钥"
   - 系统会生成一个 API Key
   - **重要**：请立即保存，只显示一次

4. **获取 API Key**
   - 格式类似：`sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 配置 DashScope API Key

```bash
export DASHSCOPE_API_KEY="sk-your-dashscope-api-key"
```

---

## 二、DeepSeek API（推荐，兼容 OpenAI）

### 如何获取 DeepSeek API Key

1. **访问 DeepSeek 开发者平台**
   - 网址：https://platform.deepseek.com/
   - 注册并登录账号

2. **申请 API Key**
   - 登录后进入"API Keys"页面
   - 点击"Create API Key"
   - 系统会生成一个 API Key
   - **重要**：请立即保存

3. **免费额度**
   - DeepSeek 提供免费试用
   - 每月有一定免费额度

### DeepSeek API Key 格式

DeepSeek API Key 格式类似：`sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 配置 DeepSeek API（推荐方案）

DeepSeek 的 API 兼容 OpenAI 格式，可以使用项目中的 `langchain4j-open-ai` 依赖。

**方案1：修改配置文件使用 DeepSeek（推荐）**

创建新的配置文件 `src/main/resources/application-deepseek.yaml`：

```yaml
langchain4j:
  open-ai:
    # LLM
    chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60s
    # Streaming LLM
    streaming-chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      timeout: 60s
    # Embedding model (DeepSeek 可能不支持，需要单独配置)
    embedding-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: text-embedding  # 需要确认 DeepSeek 是否支持

# chat config
chat:
  compress:
    enabled: true
    maxTotalTokens: 6000
    recentRawCount: 10
    minMessagesToCompress: 20
```

然后设置环境变量：

```bash
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"
export SPRING_PROFILES_ACTIVE="deepseek"
```

**方案2：临时使用 DeepSeek（快速测试）**

如果只是想快速测试，可以：
1. 设置环境变量：`export DEEPSEEK_API_KEY="your_key"`
2. 修改 `application-ai.yaml` 临时使用 DeepSeek（不推荐，会覆盖原配置）

---

## 三、快速配置脚本

### 使用 DeepSeek API

```bash
cd /projects/trip_doge

# 设置 DeepSeek API Key
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# 设置其他必需的环境变量（使用默认值）
export MYSQL_HOST="localhost"
export MYSQL_DATABASE="trip_dog"
export MYSQL_USERNAME="root"
export MYSQL_PASSWORD=""
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_DATABASE="0"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_AK="minioadmin"
export MINIO_SK="minioadmin"

# 执行测试
./配置并测试.sh
```

### 使用 DashScope API

```bash
cd /projects/trip_doge

# 设置 DashScope API Key
export DASHSCOPE_API_KEY="sk-your-dashscope-api-key"

# 设置其他环境变量...
# ...（同上）

# 执行测试
./配置并测试.sh
```

---

## 四、推荐方案

### 如果您有 DeepSeek API Key（推荐）

**优势**：
- ✅ DeepSeek API 兼容 OpenAI，配置简单
- ✅ 免费额度充足
- ✅ API 响应速度快
- ✅ 不需要修改太多代码

**配置步骤**：
1. 获取 DeepSeek API Key
2. 设置环境变量：`export DEEPSEEK_API_KEY="your_key"`
3. 创建 DeepSeek 配置文件（见下方）
4. 设置 `SPRING_PROFILES_ACTIVE=deepseek`
5. 启动服务

### 如果您有 DashScope API Key

**配置步骤**：
1. 获取 DashScope API Key
2. 设置环境变量：`export DASHSCOPE_API_KEY="your_key"`
3. 使用默认配置（`SPRING_PROFILES_ACTIVE=ai`）
4. 启动服务

---

## 五、注意事项

1. **API Key 安全**
   - ⚠️ 不要将 API Key 提交到 Git 仓库
   - ⚠️ 使用环境变量或 `.env` 文件（已加入 .gitignore）
   - ⚠️ 定期轮换 API Key

2. **费用**
   - DashScope：按使用量付费，有免费额度
   - DeepSeek：有免费额度，超出后按使用量付费

3. **功能差异**
   - DashScope：支持完整的 RAG 功能（包括 embedding）
   - DeepSeek：主要支持对话功能，embedding 可能需要单独配置

4. **测试建议**
   - 先使用 DeepSeek 进行快速测试（如果可用）
   - 生产环境建议使用 DashScope（功能更完整）

---

## 六、快速开始

### 使用 DeepSeek（推荐）

```bash
# 1. 设置 API Key
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# 2. 设置其他环境变量（使用默认值）
export MYSQL_HOST="localhost"
export MYSQL_DATABASE="trip_dog"
export MYSQL_USERNAME="root"
export REDIS_HOST="localhost"
export MINIO_ENDPOINT="http://localhost:9000"

# 3. 执行测试
cd /projects/trip_doge
./配置并测试.sh
```

### 使用 DashScope

```bash
# 1. 设置 API Key
export DASHSCOPE_API_KEY="sk-your-dashscope-api-key"

# 2. 设置其他环境变量...

# 3. 执行测试
cd /projects/trip_doge
./配置并测试.sh
```

---

**总结**：如果您有 DeepSeek API Key，推荐使用 DeepSeek，因为配置更简单且免费额度充足。如果需要完整的 RAG 功能，建议使用 DashScope。



