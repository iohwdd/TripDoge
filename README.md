# TripDoge 后端

基于 Spring Boot 3 + Java 21 + LangChain4j 的多角色 AI 对话与知识库平台，支持对话、RAG、角色管理、文档投喂，并扩展了 LangGraph4j 旅行规划工作流（Markdown 路书，SSE 进度推送）。

## 能力速览
- 多角色对话：SSE 流式聊天、上下文管理、角色配置。
- 知识库/RAG：文档上传解析、向量化检索，按用户/角色隔离。
- 旅行规划：独立接口 `/travel/plan`（SSE/JSON），节点含检索→筛选→路线→路书生成，结果以 Markdown 存 MinIO。
- 文件存储：MinIO，路书可下载/预览（Markdown）。
- 基础设施：登录与会话、路由守卫（前后端配合）。

## 项目简介
TripDoge 是一个支持多角色扮演的智能对话后端，集成文档知识库（RAG）、多角色 AI、用户体系、会话管理，并内置旅行规划 Agent 工作流。

## 技术栈
- 后端：Spring Boot 3.3.x、Java 21
- AI：LangChain4j 1.5（DashScope/通义千问）、LangGraph4j
- 数据：MySQL（主）、Redis（缓存/会话）、MinIO（对象存储）
- 可选：PostgreSQL + pgvector（向量存储）
- 其他：Apache Tika、SpringDoc OpenAPI 3.0、MapStruct、Spring Security Crypto、Spring Mail

## 主要功能
- 用户系统：注册/登录、邮箱验证码、会话管理
- AI 对话：多角色、SSE 流式、对话历史、上下文重置
- 角色管理：角色列表/详情、用户-角色会话
- 文档知识库：上传、解析、向量化、下载、删除、RAG 问答
- 旅行规划：SSE 进度推送，生成 Markdown 路书并存储 MinIO
- API 文档：Swagger/OpenAPI

## 快速开始
1) 环境准备：JDK 21、Maven 3.9+、MySQL、Redis、MinIO、DashScope API Key（可选 pgvector）。
2) 配置环境变量（示例，详见 `docs/TripDoge项目启动配置说明.md`）：
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
3) 初始化数据库：`mysql -u root -p < sql/init.sql`
4) 启动：`mvn spring-boot:run` 或 `mvn clean package -DskipTests && java -jar target/*.jar`

## 关键接口
- 对话：`/api/chat/{roleId}`（SSE）
- 旅行规划：`/travel/plan`（SSE/JSON，返回 historyId、mdPath、mdUrl）
- 文档/RAG：`/api/doc/*`
- 用户/会话：`/api/user/*`
- Swagger：`/swagger-ui/index.html`

## 目录结构
```
src/main/java/com/tripdog/
├── controller/   # 控制器（含 /travel/plan）
├── service/      # 业务与工作流封装
├── ai/           # LangGraph 工作流、Retriever
├── model/        # 实体/DTO/VO
├── mapper/       # MyBatis 映射
└── ...
src/main/resources/mapper/  # XML 映射
sql/                       # 初始化脚本
docs/                      # 配置与功能说明
```

## 依赖服务
- MySQL（主数据）
- Redis（缓存/会话）
- MinIO（对象存储）
- LangChain4j（AI 服务）
- 可选 PostgreSQL+pgvector（文档向量）
- SMTP（邮件验证码）

## 常见问题
- 启动失败：优先检查数据库、Redis、MinIO、DashScope 等连接与配置。
- 更多排查与环境变量说明参见 `docs/TripDoge项目启动配置说明.md`。

## 前端简览
<img width="3024" height="1522" alt="15e6ca330c855948037b02c640a522a3" src="https://github.com/user-attachments/assets/4d687317-2788-482b-8409-cd6fa764c2d9" />
<img width="3024" height="1522" alt="15e6ca330c855948037b02c640a522a3" src="https://github.com/user-attachments/assets/df937ba6-7cc9-4975-94c9-7224dc640bf5" />
<img width="3024" height="1522" alt="4be7ab665bb8915d005d229b59924606" src="https://github.com/user-attachments/assets/ab64f68f-8636-4c73-ac49-d054ec68708a" />
<img width="3024" height="1522" alt="4be7ab665bb8915d005d229b59924606" src="https://github.com/user-attachments/assets/ac2e226e-64d6-4bb2-8843-33e671b0c04b" />


## 贡献与维护
- 欢迎 PR 与 Issue
- 文档更新时间：2025年12月12日

更多细节、配置与完整指引请查看 `docs/`，此 README 在保留原始信息的基础上补充了旅行规划与 SSE 相关概览。***
