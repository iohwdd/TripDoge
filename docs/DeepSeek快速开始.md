# DeepSeek API 快速开始指南

## 步骤1：获取 DeepSeek API Key

1. **访问 DeepSeek 开发者平台**
   - 网址：https://platform.deepseek.com/
   - 注册并登录账号

2. **创建 API Key**
   - 登录后进入 "API Keys" 页面
   - 点击 "Create API Key"
   - 复制生成的 API Key（格式：`sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`）

## 步骤2：设置环境变量并运行测试

### 方法1：快速测试（推荐）

```bash
cd /projects/trip_doge

# 设置 DeepSeek API Key
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# 运行快速测试脚本
./快速测试DeepSeek.sh
```

### 方法2：交互式配置

```bash
cd /projects/trip_doge

# 运行交互式脚本（会提示输入 API Key）
./使用DeepSeek测试.sh
```

### 方法3：手动配置

```bash
cd /projects/trip_doge

# 1. 设置 DeepSeek API Key
export DEEPSEEK_API_KEY="sk-your-deepseek-api-key"

# 2. 设置使用 DeepSeek 配置
export SPRING_PROFILES_ACTIVE="deepseek"

# 3. 设置其他环境变量（使用默认值）
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

# 4. 执行测试
./配置并测试.sh
```

## 步骤3：验证测试结果

测试脚本会自动：
1. ✅ 检查环境变量配置
2. ✅ 编译后端和前端代码
3. ✅ 启动后端服务
4. ✅ 执行 API 测试
5. ✅ 启动前端服务

测试完成后，查看测试结果：
- 测试日志：`/tmp/test_result.md`
- 后端日志：`/tmp/backend_test.log`
- 前端日志：`/tmp/frontend_test.log`

## 常见问题

### Q1: 如何获取 DeepSeek API Key？

A: 访问 https://platform.deepseek.com/，注册账号后进入 "API Keys" 页面创建。

### Q2: DeepSeek API Key 格式是什么？

A: 格式类似：`sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### Q3: 测试失败怎么办？

A: 检查：
1. API Key 是否正确
2. 网络连接是否正常
3. 依赖服务（MySQL、Redis）是否运行
4. 查看日志文件排查错误

### Q4: 可以使用 DashScope 吗？

A: 可以。设置 `DASHSCOPE_API_KEY` 环境变量，并使用默认配置（`SPRING_PROFILES_ACTIVE=ai`）。

## 下一步

配置完成后，测试脚本会自动执行完整测试验证。如果测试通过，您可以：

1. 访问前端：http://localhost:5173
2. 访问后端 API 文档：http://localhost:7979/api/swagger-ui/index.html
3. 开始使用系统

---

**提示**：DeepSeek 提供免费额度，非常适合测试使用！

