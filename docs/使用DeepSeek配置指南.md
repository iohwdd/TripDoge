# 使用 DeepSeek API 配置指南

## 快速配置 DeepSeek API

### 步骤1：获取 DeepSeek API Key

1. 访问 https://platform.deepseek.com/
2. 注册/登录账号
3. 进入 "API Keys" 页面
4. 创建新的 API Key
5. 复制 API Key（格式：`sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`）

### 步骤2：配置环境变量

```bash
cd /projects/trip_doge

# 设置 DeepSeek API Key（必需）
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# 设置 Spring Profile 使用 DeepSeek 配置
export SPRING_PROFILES_ACTIVE="deepseek"

# 设置其他必需的环境变量（使用默认值）
export MYSQL_HOST="localhost"
export MYSQL_DATABASE="trip_dog"
export MYSQL_USERNAME="root"
export MYSQL_PASSWORD=""  # 如果 MySQL 无密码
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_DATABASE="0"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_AK="minioadmin"
export MINIO_SK="minioadmin"
```

### 步骤3：启动服务

```bash
cd /projects/trip_doge
mvn spring-boot:run
```

### 步骤4：验证配置

启动后检查日志，应该看到：
- 使用 `application-deepseek.yaml` 配置
- 连接到 `https://api.deepseek.com/v1`
- 使用模型 `deepseek-chat`

---

## 注意事项

### ✅ 优势

1. **免费额度充足**：DeepSeek 提供免费试用
2. **配置简单**：兼容 OpenAI 格式，无需修改代码
3. **响应快速**：API 响应速度快

### ⚠️ 限制

1. **Embedding 功能**：DeepSeek 可能不支持 embedding，RAG 功能可能受限
2. **MCP 功能**：MCP（Model Context Protocol）功能可能不可用
3. **功能完整性**：相比 DashScope，功能可能不完整

### 💡 建议

- **测试环境**：推荐使用 DeepSeek（免费、快速）
- **生产环境**：建议使用 DashScope（功能完整）

---

## 快速测试命令

```bash
# 一键配置并测试
cd /projects/trip_doge
export DEEPSEEK_API_KEY="sk-your-key"
export SPRING_PROFILES_ACTIVE="deepseek"
export MYSQL_HOST="localhost"
export MYSQL_DATABASE="trip_dog"
export MYSQL_USERNAME="root"
export REDIS_HOST="localhost"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_AK="minioadmin"
export MINIO_SK="minioadmin"

# 执行测试
./配置并测试.sh
```

---

## 切换回 DashScope

如果需要切换回 DashScope：

```bash
# 设置 DashScope API Key
export DASHSCOPE_API_KEY="sk-your-dashscope-key"

# 使用默认配置（ai profile）
export SPRING_PROFILES_ACTIVE="ai"
# 或者不设置，默认就是 "ai"

# 启动服务
mvn spring-boot:run
```

---

## 故障排查

### 问题1：找不到 DEEPSEEK_API_KEY

**错误**：`Could not resolve placeholder 'DEEPSEEK_API_KEY'`

**解决**：
```bash
export DEEPSEEK_API_KEY="your-key"
export SPRING_PROFILES_ACTIVE="deepseek"
```

### 问题2：API 调用失败

**检查**：
1. API Key 是否正确
2. 网络连接是否正常
3. DeepSeek 服务是否可用

### 问题3：Embedding 功能不可用

**原因**：DeepSeek 可能不支持 embedding

**解决**：使用 DashScope 或配置其他 embedding 服务

---

**总结**：DeepSeek API 可以用于快速测试，配置简单且免费。如果需要完整功能，建议使用 DashScope。

