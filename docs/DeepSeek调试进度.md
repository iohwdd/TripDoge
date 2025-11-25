# DeepSeek 配置调试进度

**调试时间**: 2025-11-24  
**API Key**: sk-ee92429c3cea41b2b9d04037c2cd4a2b

---

## 已完成的修复

### ✅ 1. 修复 base-url
- **问题**: base-url 格式错误
- **修复**: 从 `https://api.deepseek.com` 改为 `https://api.deepseek.com/v1`

### ✅ 2. 修复 timeout 格式
- **问题**: timeout 值 `60s` 无法转换为 Integer
- **修复**: 从 `60s` 改为 `60`（秒数）

### ✅ 3. 创建手动 Bean 配置
- **创建**: `DeepSeekConfig.java` - 手动创建 StreamingChatModel Bean
- **创建**: `DeepSeekChatModelConfig.java` - 手动创建 ChatModel Bean
- **原因**: LangChain4j 自动配置无法识别 DeepSeek 配置

### ✅ 4. 添加 PgVector 配置
- **问题**: EmbeddingStore 需要 PgVector 配置
- **修复**: 在 `application-deepseek.yaml` 中添加 PgVector 配置
- **添加**: 默认值配置，支持环境变量覆盖

---

## 当前问题

### ⚠️ PgVector 连接问题

**错误信息**:
```
host cannot be null or blank
```

**可能原因**:
1. PgVector 服务未运行
2. 环境变量未正确传递到配置
3. 配置属性读取问题

**调试步骤**:
1. ✅ 已添加 PgVector 配置到 `application-deepseek.yaml`
2. ✅ 已设置环境变量默认值
3. ⏳ 需要验证环境变量是否正确传递

---

## 配置状态

### 环境变量
- ✅ DEEPSEEK_API_KEY: 已设置
- ✅ SPRING_PROFILES_ACTIVE: deepseek
- ✅ PGVECTOR_HOST: localhost
- ✅ PGVECTOR_PORT: 5432
- ✅ PGVECTOR_DATABASE: trip_dog_vector
- ✅ PGVECTOR_USER: postgres
- ✅ PGVECTOR_PASSWORD: postgres
- ✅ PGVECTOR_TABLE: document_embeddings

### 配置文件
- ✅ `application-deepseek.yaml`: 已创建并配置
- ✅ `DeepSeekConfig.java`: 已创建
- ✅ `DeepSeekChatModelConfig.java`: 已创建

---

## 下一步调试方向

### 方案1：检查 PgVector 服务
```bash
# 检查 PostgreSQL 是否运行
psql -h localhost -U postgres -d trip_dog_vector -c "SELECT 1"
```

### 方案2：使用内存 EmbeddingStore（临时方案）
如果不需要 RAG 功能，可以创建一个内存 EmbeddingStore：
- 创建 `InMemoryEmbeddingStore` Bean
- 条件配置：当 PgVector 不可用时使用内存存储

### 方案3：禁用 RAG 功能（最简单）
如果只是为了测试 API 修复，可以：
- 修改 `AssistantService` 使其在没有 EmbeddingStore 时也能工作
- 或者创建一个空的 EmbeddingStore

---

## 建议

**为了快速完成测试验证**，建议：

1. **临时方案**：使用 DashScope API（已验证可用）
2. **继续调试**：修复 PgVector 连接问题
3. **简化方案**：创建内存 EmbeddingStore 或禁用 RAG

---

**当前状态**: DeepSeek 配置已基本完成，但需要解决 PgVector 连接问题。

