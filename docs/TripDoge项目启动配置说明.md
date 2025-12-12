# TripDoge 项目启动配置说明

## 概述

TripDoge 后端项目基于 Spring Boot 3.3.2 + Java 21，默认端口 7979，API路径 `/api`。

## 环境配置

通过 `SPRING_PROFILES_ACTIVE` 指定环境：`ai`(默认) | `prod` | `test`

## 环境变量配置

### 必需配置

```bash
export REDIS_HOST=""
export REDIS_PORT=""
export REDIS_PASSWORD=""

export MYSQL_HOST=""
export MYSQL_PORT=""
export MYSQL_DATABASE=""
export MYSQL_USERNAME=""
export MYSQL_PASSWORD=""

export MINIO_ENDPOINT=""
export MINIO_PORT=""
export MINIO_AK=""
export MINIO_SK=""

export PGVECTOR_HOST=""
export PGVECTOR_PORT=""
export PGVECTOR_DATABASE=""
export PGVECTOR_USER=""
export PGVECTOR_PASSWORD=""
export PGVECTOR_TABLE=""

export DASHSCOPE_API_KEY=""
export SEARCH_MCP_LINK=""
export MAP_MCP_LINK=""

export RABBITMQ_HOST=""

export MAIL_PORT="465"
export MAIL_USERNAME=""
export MAIL_PASSWORD=""
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
