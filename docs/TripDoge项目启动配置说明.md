# TripDoge 项目启动配置说明

## 概述

TripDoge 后端项目基于 Spring Boot 3.3.2 + Java 21，默认端口 7979，API路径 `/api`。

## 环境配置

通过 `SPRING_PROFILES_ACTIVE` 指定环境：`ai`(默认) | `prod` | `test`

## 环境变量配置

### 必需配置

```bash
# MySQL 数据库 (数据库名:trip_dog)
MYSQL_PASSWORD=your_password      # (必需)
MYSQL_HOST=localhost              # 默认localhost
MYSQL_PORT=3306                   # 默认3306
MYSQL_USERNAME=root               # 默认root

# Redis 缓存和会话
REDIS_HOST=your_redis_host        # (必需)
REDIS_PORT=6379                   # (必需)
REDIS_PASSWORD=your_password      # (必需)
REDIS_DATABASE=0                  # (必需)
# 会话本地fallback（Redis断链时自动启用）
SESSION_FALLBACK_ENABLED=true
SESSION_FALLBACK_MAX_SIZE=1000
SESSION_FALLBACK_CLEANUP_INTERVAL_MS=60000

# AI 服务 (DashScope通义千问)
DASHSCOPE_API_KEY=your_api_key    # (必需)
# 使用模型: qwen-plus (对话和流式)、text-embedding-v3 (嵌入)

# MinIO 对象存储 (存储桶:trip-doge)
MINIO_ENDPOINT=your_endpoint      # (必需)
MINIO_AK=your_access_key         # (必需)
MINIO_SK=your_secret_key         # (必需)

# 邮件服务 (注册验证码)
MAIL_USERNAME=your_email          # (必需)
MAIL_PASSWORD=your_auth_code      # (必需)
```

### 可选配置

```bash
SERVER_PORT=7979                  # 服务端口，默认7979
MAIL_HOST=smtp.qq.com            # SMTP服务器，默认QQ邮箱
MAIL_PORT=465                    # SMTP端口，默认465

# PostgreSQL向量数据库 (用于文档向量存储)
PGVECTOR_HOST=localhost          # 默认localhost
PGVECTOR_PORT=5432              # 默认5432
PGVECTOR_DATABASE=trip_vdb      # 默认trip_vdb
PGVECTOR_USER=postgres          # 默认postgres
PGVECTOR_PASSWORD=postgres      # 默认postgres
PGVECTOR_TABLE=vectors_db       # 默认vectors_db

# 跨域配置
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080  # 允许的前端域名
CORS_ALLOW_CREDENTIALS=true     # 是否允许凭证，默认true

# 其他配置(已有默认值，一般无需修改)
# 会话fallback也可通过以下变量覆盖默认值
# SESSION_FALLBACK_ENABLED=true
# SESSION_FALLBACK_MAX_SIZE=1000
# SESSION_FALLBACK_CLEANUP_INTERVAL_MS=60000
# SERVER_IP=localhost            # 服务器IP，用于PostgreSQL连接默认值
```

## 服务地址

- **应用**: `http://localhost:7979/api`
- **API文档**: `http://localhost:7979/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:7979/api/v3/api-docs`
- **健康检查**: `http://localhost:7979/api/actuator/health` (仅prod环境)

## 启动方式

### 开发环境

```bash
# 设置必需环境变量后启动
mvn spring-boot:run
```

### 生产环境

```bash
export SPRING_PROFILES_ACTIVE=ai,prod
mvn clean package -DskipTests
java -jar target/tripdog-backend-1.0.0.jar
```

## 依赖服务

**必需服务**:

- MySQL 8.0+ (数据库名: trip_dog)
- Redis (会话存储和缓存)
- DashScope API (阿里云通义千问)
- MinIO (对象存储，存储桶: trip-doge)

**可选服务**:

- PostgreSQL + pgvector (文档向量存储，数据库名: trip_vdb)
- SMTP邮件服务 (用户注册验证码)

## 故障排除

**启动失败常见原因**:

- **MySQL连接失败**: 检查服务状态、数据库名(trip_dog)、用户名密码
- **Redis连接失败**: 检查服务状态、主机端口、密码配置
- **DashScope API失败**: 检查API Key有效性、网络连接、账户余额
- **MinIO连接失败**: 检查服务状态、endpoint配置、访问密钥
- **邮件发送失败**: 检查SMTP配置、邮箱授权码、SSL设置
- **文档向量化失败**: 检查PostgreSQL服务、pgvector插件安装

---

**文档版本**: v1.0 | **更新时间**: 2025年9月28日
