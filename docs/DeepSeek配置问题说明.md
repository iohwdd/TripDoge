# DeepSeek 配置问题说明

## 问题

后端服务启动失败，错误信息：
```
No qualifying bean of type 'dev.langchain4j.model.chat.StreamingChatModel' available
```

## 原因分析

LangChain4j 的自动配置可能无法正确识别 DeepSeek 配置。可能的原因：

1. **配置格式问题**：`langchain4j-open-ai` 的配置格式可能与预期不符
2. **自动配置问题**：Spring Boot Starter 可能无法自动创建 bean
3. **依赖问题**：可能需要额外的依赖或配置

## 解决方案

### 方案1：使用 DashScope（推荐，功能完整）

如果只是为了测试 API 修复，建议使用 DashScope：

```bash
# 获取 DashScope API Key
# 访问：https://dashscope.console.aliyun.com/

# 设置环境变量
export DASHSCOPE_API_KEY="your_dashscope_key"
export SPRING_PROFILES_ACTIVE="ai"  # 或留空，默认就是 ai

# 启动服务
mvn spring-boot:run
```

### 方案2：修复 DeepSeek 配置（需要进一步调试）

需要检查 LangChain4j 1.5.0 的正确配置格式，可能需要：
1. 调整配置格式
2. 添加手动 Bean 配置
3. 检查依赖版本兼容性

## 当前状态

- ✅ 环境变量已配置
- ✅ 代码编译通过
- ❌ 后端服务启动失败（StreamingChatModel bean 未找到）

## 建议

**为了快速完成测试验证，建议使用 DashScope API**：
1. 功能完整（支持 RAG、Embedding）
2. 配置已验证可用
3. 可以快速完成测试

如果需要继续调试 DeepSeek 配置，需要：
1. 查看 LangChain4j 文档
2. 检查配置格式
3. 可能需要手动创建 Bean

---

**总结**：DeepSeek 配置需要进一步调试。建议先使用 DashScope 完成测试验证。

