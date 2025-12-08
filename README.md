# TripDoge

基于 **LangChain4j 1.5.0** 的多角色AI对话平台，支持智能对话、知识库问答、文档管理、用户系统等功能。适用于陪伴、问答、知识管理等多场景。

## 项目简介

TripDoge 是一个支持多角色扮演的智能对话后端，集成了文档知识库（RAG）、多角色AI、用户体系、会话管理等能力。用户可与不同AI角色互动，上传文档构建专属知识库，实现智能问答。

## 技术栈

- **后端框架**：Spring Boot 3.3.2
- **语言**：Java 21
- **AI引擎**：LangChain4j 1.5.0、DashScope（通义千问）
- **数据库**：MySQL（主数据）、PostgreSQL+pgvector（向量存储）
- **缓存/会话**：Redis
- **对象存储**：MinIO
- **文档解析**：Apache Tika
- **API文档**：SpringDoc OpenAPI 3.0
- **其他**：MapStruct、Spring Security Crypto、Spring Mail

## 主要功能

- **用户系统**：注册、登录、邮箱验证码、会话管理
- **AI对话**：多角色AI、SSE流式对话、对话历史、上下文重置
- **角色管理**：角色列表、角色详情、用户-角色会话
- **文档知识库**：文档上传、解析、向量化、下载、删除、RAG问答
- **API文档**：自动生成Swagger文档

## 快速开始

### 环境准备

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis
- MinIO
- DashScope API Key（通义千问）
- 可选：PostgreSQL + pgvector（文档向量存储）

### 配置环境变量

参考 [`docs/TripDoge项目启动配置说明.md`](docs/TripDoge项目启动配置说明.md) 配置以下环境变量：

```bash
export REDIS_HOST=""
export REDIS_PORT=""
export REDIS_PASSWORD=""
export REDIS_DATABASE=""

export MYSQL_HOST=""
export MYSQL_PORT=""
export MYSQL_DATABASE=""
export MYSQL_USERNAME=""
export MYSQL_PASSWORD=""

export MAIL_PORT="465"
export MAIL_USERNAME=""
export MAIL_PASSWORD=""

export MINIO_ENDPOINT=""
export MINIO_PORT=""
export MINIO_AK=""
export MINIO_SK=""

export PGVECTOR_DATABASE=""
export PGVECTOR_USER=""
export PGVECTOR_PASSWORD=""
export PGVECTOR_TABLE=""

export DASHSCOPE_API_KEY=""

export SEARCH_MCP_LINK=""

export RABBITMQ_HOST=""

export PGVECTOR_HOST=""
export PGVECTOR_PORT=""
export PGVECTOR_DATABASE=""
export PGVECTOR_USER=""
export PGVECTOR_PASSWORD=""
export PGVECTOR_TABLE=""
```

### 数据库初始化

```bash
# 初始化MySQL
mysql -u root -p < sql/init.sql
```

### 本地启动

```bash
# 启动开发环境
mvn spring-boot:run
# 或打包后运行
mvn clean package -DskipTests
java -jar target/tripdog-backend-1.0.0.jar
```

## API接口概览

- 用户注册/登录/登出/信息：`/api/user/*`
- AI对话（SSE流式）：`/api/chat/{roleId}`
- 角色管理：`/api/roles/list`
- 文档管理：`/api/doc/parse`、`/api/doc/list`、`/api/doc/download`、`/api/doc/delete`
- Swagger文档：`/swagger-ui/index.html`

详细接口说明见 [`docs/TripDoge功能模块.md`](docs/TripDoge功能模块.md)。

## 目录结构

```
├── src/
│   ├── main/
│   │   ├── java/com/tripdog/
│   │   │   ├── controller/   # 控制器
│   │   │   ├── service/      # 业务逻辑
│   │   │   ├── model/        # 实体/VO/DTO
│   │   │   ├── mapper/       # MyBatis映射
│   │   │   └── ...           # 其他
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── mapper/*.xml
│   └── test/
├── docs/                     # 项目文档
├── sql/                      # 数据库脚本
├── logs/                     # 日志
├── Dockerfile
├── deploy.sh
└── pom.xml
```

## 依赖服务

- MySQL（主数据）
- Redis（缓存/会话）
- MinIO（对象存储）
- Langchain4j（AI服务）
- PostgreSQL+pgvector（文档向量，选配）
- SMTP（邮件验证码）

## 常见问题

- 启动失败请检查数据库、Redis、MinIO、DashScope等服务连接与配置
- 详细排查与环境变量说明见 [`docs/TripDoge项目启动配置说明.md`](docs/TripDoge项目启动配置说明.md)

## 贡献与维护

- 欢迎PR和Issue
- 文档版本：v1.0（2025年9月28日）

---

如需更详细的功能、接口、部署说明，请查阅 `docs/` 目录下的文档。
