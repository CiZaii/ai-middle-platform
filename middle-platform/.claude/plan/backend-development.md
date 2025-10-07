# AI知识库管理系统 - 后端开发规划文档

> **项目名称**: AI Middle Platform - Knowledge Base Management System
> **技术栈**: Spring Boot 3.2.x + Spring AI + Neo4j + PostgreSQL + RabbitMQ + Redis + x-file-storage
> **架构模式**: MVC + 异步消息处理
> **文档版本**: v1.0
> **创建日期**: 2025-10-03

---

## 📋 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [数据库设计](#3-数据库设计)
4. [API接口设计](#4-api接口设计)
5. [异步处理流程](#5-异步处理流程)
6. [文件存储方案](#6-文件存储方案)
7. [Spring AI集成](#7-spring-ai集成)
8. [实施计划](#8-实施计划)
9. [初始化脚本](#9-初始化脚本)

---

## 1. 项目概述

### 1.1 系统功能

AI知识库管理系统是一个智能文档处理平台，提供以下核心功能：

- **知识库管理**: 创建、查询、删除知识库，支持成员协作
- **文件管理**: 上传文档（PDF/Word/图片），文件元数据管理
- **智能处理**: OCR识别、文本向量化、知识图谱生成、问答对提取
- **模型配置**: AI端点管理、API Key管理、业务配置
- **用户管理**: 用户认证、角色权限管理

### 1.2 技术架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端 (Next.js)                           │
│                    React Query + Axios                          │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot 3.2.x)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Controller   │  │   Service    │  │  Repository  │          │
│  │   (REST)     │  │  (业务逻辑)   │  │ (MyBatis-Plus)│         │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Spring AI   │  │   RabbitMQ   │  │  x-file-     │          │
│  │  (AI调用)     │  │  (异步处理)   │  │  storage     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  PostgreSQL  │    │    Neo4j     │    │    Redis     │
│  (主数据)     │    │  (知识图谱)   │    │   (缓存)     │
└──────────────┘    └──────────────┘    └──────────────┘
                             ▼
                    ┌──────────────┐
                    │  S3 Storage  │
                    │  (文件存储)   │
                    └──────────────┘
```

### 1.3 核心流程

**文件上传处理流程**:
1. 前端上传文件 → 2. x-file-storage存储到S3 → 3. 返回文件URL → 4. 保存元数据到PostgreSQL → 5. 发送MQ消息 → 6. 异步处理（OCR/向量化/图谱/问答对） → 7. 更新处理状态

---

## 2. 技术架构

### 2.1 技术选型

| 技术组件 | 版本 | 用途 |
|---------|------|------|
| Spring Boot | 3.2.x | 核心框架 |
| Spring AI | 1.0.0-M4 | AI模型集成 |
| Spring Security | 3.2.x | 认证授权（JWT） |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| PostgreSQL | 15+ | 关系数据库 + pgvector |
| Neo4j | 5.x | 图数据库 |
| RabbitMQ | 3.12.x | 消息队列 |
| Redis | 7.x | 缓存 + 分布式锁 |
| x-file-storage | 2.1.0 | 文件存储（S3） |

### 2.2 项目结构

```
src/main/java/com/ai/middle/platform/
├── config/                    # 配置类
│   ├── SecurityConfig.java
│   ├── RabbitMQConfig.java
│   ├── Neo4jConfig.java
│   ├── FileStorageConfig.java
│   └── SpringAIConfig.java
├── controller/                # 控制器层
│   ├── KnowledgeBaseController.java
│   ├── FileController.java
│   ├── EndpointController.java
│   ├── ApiKeyController.java
│   ├── BusinessController.java
│   └── UserController.java
├── service/                   # 服务层
│   ├── KnowledgeBaseService.java
│   ├── FileService.java
│   ├── AIProcessService.java
│   ├── ModelConfigService.java
│   └── UserService.java
├── repository/                # 数据访问层
│   ├── mapper/               # MyBatis-Plus Mapper
│   └── neo4j/                # Neo4j Repository
├── entity/                    # 实体类
│   ├── po/                   # PostgreSQL实体
│   └── graph/                # Neo4j图实体
├── dto/                       # 数据传输对象
├── mq/                        # 消息队列
│   ├── producer/             # 消息生产者
│   └── consumer/             # 消息消费者
└── common/                    # 通用组件
    ├── enums/                # 枚举
    ├── exception/            # 异常处理
    └── utils/                # 工具类
```

---

## 3. 数据库设计

### 3.1 PostgreSQL 表结构设计

#### 3.1.1 用户表 (sys_user)

```sql
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'user', -- 'admin' | 'user'
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_role ON sys_user(role);
COMMENT ON TABLE sys_user IS '用户表';
```

#### 3.1.2 知识库表 (kb_knowledge_base)

```sql
CREATE TABLE kb_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    kb_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES sys_user(id),
    file_count INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kb_owner ON kb_knowledge_base(owner_id);
CREATE INDEX idx_kb_created ON kb_knowledge_base(created_at DESC);
COMMENT ON TABLE kb_knowledge_base IS '知识库表';
```

#### 3.1.3 知识库成员表 (kb_member)

```sql
CREATE TABLE kb_member (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'owner' | 'editor' | 'viewer'
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, user_id)
);

CREATE INDEX idx_member_kb ON kb_member(kb_id);
CREATE INDEX idx_member_user ON kb_member(user_id);
COMMENT ON TABLE kb_member IS '知识库成员表';
```

#### 3.1.4 文件表 (kb_file)

```sql
CREATE TABLE kb_file (
    id BIGSERIAL PRIMARY KEY,
    file_id VARCHAR(50) NOT NULL UNIQUE,
    kb_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL, -- 'pdf' | 'word' | 'image'
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL, -- S3路径
    url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000),
    uploaded_by BIGINT NOT NULL REFERENCES sys_user(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 处理状态
    ocr_status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending'|'processing'|'completed'|'failed'
    vectorization_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    qa_pairs_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    knowledge_graph_status VARCHAR(20) NOT NULL DEFAULT 'pending',

    -- OCR结果（Markdown格式）
    ocr_content TEXT,

    -- 错误信息
    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_file_kb ON kb_file(kb_id);
CREATE INDEX idx_file_uploaded_by ON kb_file(uploaded_by);
CREATE INDEX idx_file_type ON kb_file(file_type);
CREATE INDEX idx_file_ocr_status ON kb_file(ocr_status);
COMMENT ON TABLE kb_file IS '文件表';
```

#### 3.1.5 向量表 (kb_vector)

```sql
-- 需要先启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kb_vector (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI embedding维度
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, chunk_index)
);

CREATE INDEX idx_vector_file ON kb_vector(file_id);
CREATE INDEX idx_vector_embedding ON kb_vector USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
COMMENT ON TABLE kb_vector IS '向量表（文档片段）';
```

#### 3.1.6 问答对表 (kb_qa_pair)

```sql
CREATE TABLE kb_qa_pair (
    id BIGSERIAL PRIMARY KEY,
    qa_id VARCHAR(50) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_text TEXT NOT NULL,
    confidence_score DECIMAL(5,4), -- 置信度 0-1
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_qa_file ON kb_qa_pair(file_id);
CREATE INDEX idx_qa_question ON kb_qa_pair USING gin(to_tsvector('simple', question));
COMMENT ON TABLE kb_qa_pair IS '问答对表';
```

#### 3.1.7 业务配置表 (model_business)

```sql
CREATE TABLE model_business (
    id BIGSERIAL PRIMARY KEY,
    business_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_business_code ON model_business(code);
COMMENT ON TABLE model_business IS '业务配置表';
```

#### 3.1.8 端点配置表 (model_endpoint)

```sql
CREATE TABLE model_endpoint (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'OpenAI'|'Claude'|'Gemini'|'Azure'|'Custom'
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_endpoint_provider ON model_endpoint(provider);
COMMENT ON TABLE model_endpoint IS '端点配置表';
```

#### 3.1.9 模型表 (model_info)

```sql
CREATE TABLE model_info (
    id BIGSERIAL PRIMARY KEY,
    model_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_model_endpoint ON model_info(endpoint_id);
COMMENT ON TABLE model_info IS '模型信息表';
```

#### 3.1.10 端点业务关联表 (model_endpoint_business)

```sql
CREATE TABLE model_endpoint_business (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES model_business(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(endpoint_id, business_id)
);

CREATE INDEX idx_eb_endpoint ON model_endpoint_business(endpoint_id);
CREATE INDEX idx_eb_business ON model_endpoint_business(business_id);
COMMENT ON TABLE model_endpoint_business IS '端点业务关联表';
```

#### 3.1.11 API Key表 (model_api_key)

```sql
CREATE TABLE model_api_key (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    api_key VARCHAR(500) NOT NULL, -- 加密存储
    display_key VARCHAR(100) NOT NULL, -- 脱敏显示
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 限流配置
    rate_limit_per_minute INT,
    rate_limit_per_day INT,

    -- 统计信息
    total_requests BIGINT NOT NULL DEFAULT 0,
    success_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    last_error TEXT,

    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_apikey_endpoint ON model_api_key(endpoint_id);
CREATE INDEX idx_apikey_enabled ON model_api_key(enabled);
COMMENT ON TABLE model_api_key IS 'API Key配置表';
```

#### 3.1.12 处理任务表 (kb_process_task)

```sql
CREATE TABLE kb_process_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL, -- 'OCR'|'VECTORIZATION'|'QA_GENERATION'|'KNOWLEDGE_GRAPH'
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending'|'processing'|'completed'|'failed'
    progress INT NOT NULL DEFAULT 0, -- 0-100
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_file ON kb_process_task(file_id);
CREATE INDEX idx_task_status ON kb_process_task(status);
CREATE INDEX idx_task_type ON kb_process_task(task_type);
COMMENT ON TABLE kb_process_task IS '处理任务表';
```

### 3.2 Neo4j 图数据库设计

#### 3.2.1 节点类型 (Node Labels)

```cypher
// 文档节点
(:Document {
    id: String,           // 文件ID
    name: String,
    type: String,
    createdAt: DateTime
})

// 概念节点
(:Concept {
    id: String,
    name: String,
    type: String,         // 'entity' | 'topic' | 'keyword'
    description: String
})

// 实体节点
(:Entity {
    id: String,
    name: String,
    type: String,         // 'person' | 'organization' | 'location' | 'product' | 'technology'
    attributes: Map
})

// 章节节点
(:Section {
    id: String,
    title: String,
    level: Int,           // 1-6
    content: String
})
```

#### 3.2.2 关系类型 (Relationship Types)

```cypher
// 文档包含章节
(:Document)-[:CONTAINS]->(:Section)

// 章节包含概念
(:Section)-[:MENTIONS]->(:Concept)

// 概念之间的关系
(:Concept)-[:RELATED_TO {strength: Float}]->(:Concept)

// 实体之间的关系
(:Entity)-[:INTERACTS_WITH {type: String, description: String}]->(:Entity)

// 概念定义实体
(:Concept)-[:DEFINES]->(:Entity)

// 实体属于文档
(:Entity)-[:BELONGS_TO]->(:Document)

// 层级关系
(:Section)-[:PARENT_OF]->(:Section)
```

#### 3.2.3 索引和约束

```cypher
// 唯一约束
CREATE CONSTRAINT doc_id IF NOT EXISTS FOR (d:Document) REQUIRE d.id IS UNIQUE;
CREATE CONSTRAINT concept_id IF NOT EXISTS FOR (c:Concept) REQUIRE c.id IS UNIQUE;
CREATE CONSTRAINT entity_id IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE;
CREATE CONSTRAINT section_id IF NOT EXISTS FOR (s:Section) REQUIRE s.id IS UNIQUE;

// 全文搜索索引
CREATE FULLTEXT INDEX concept_search IF NOT EXISTS
FOR (c:Concept) ON EACH [c.name, c.description];

CREATE FULLTEXT INDEX entity_search IF NOT EXISTS
FOR (e:Entity) ON EACH [e.name, e.attributes];
```

---

## 4. API接口设计

### 4.1 认证接口

#### POST /api/auth/login
```json
// Request
{
  "email": "user@example.com",
  "password": "password123"
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": "1",
      "name": "张三",
      "email": "user@example.com",
      "role": "admin"
    }
  }
}
```

### 4.2 知识库接口

#### GET /api/knowledge-bases
查询知识库列表

```json
// Response
{
  "code": 200,
  "data": [
    {
      "id": "kb-1",
      "name": "技术文档库",
      "description": "存储技术文档",
      "fileCount": 45,
      "owner": {
        "id": "1",
        "name": "张三",
        "email": "user@example.com",
        "role": "admin"
      },
      "members": [],
      "createdAt": "2024-01-15T08:00:00Z",
      "updatedAt": "2024-01-20T10:30:00Z"
    }
  ]
}
```

#### GET /api/knowledge-bases/{id}
查询知识库详情

#### POST /api/knowledge-bases
创建知识库

```json
// Request
{
  "name": "新知识库",
  "description": "描述信息"
}
```

#### DELETE /api/knowledge-bases/{id}
删除知识库

### 4.3 文件接口

#### GET /api/knowledge-bases/{kbId}/files
查询文件列表

```json
// Response
{
  "code": 200,
  "data": [
    {
      "id": "file-1",
      "knowledgeBaseId": "kb-1",
      "name": "Next.js官方文档.pdf",
      "type": "pdf",
      "mimeType": "application/pdf",
      "size": 5242880,
      "uploadedAt": "2024-01-15T10:00:00Z",
      "uploadedBy": {
        "id": "1",
        "name": "张三"
      },
      "statuses": {
        "ocr": "completed",
        "vectorization": "processing",
        "qaPairs": "pending",
        "knowledgeGraph": "pending"
      },
      "url": "https://s3.example.com/files/xxx.pdf",
      "thumbnailUrl": "https://s3.example.com/thumbnails/xxx.jpg"
    }
  ]
}
```

#### GET /api/files/{id}
查询文件详情（包含OCR内容、知识图谱、问答对）

```json
// Response
{
  "code": 200,
  "data": {
    "id": "file-1",
    "name": "API接口文档.pdf",
    "ocrContent": "# API 接口文档\n\n## 用户认证 API...",
    "knowledgeGraph": {
      "nodes": [
        {"id": "1", "label": "API接口文档", "type": "document"},
        {"id": "2", "label": "用户认证", "type": "module"}
      ],
      "edges": [
        {"source": "1", "target": "2", "label": "包含"}
      ]
    },
    "qaPairs": [
      {
        "id": "qa-1",
        "question": "用户登录接口的URL是什么?",
        "answer": "用户登录接口的URL是 POST /api/auth/login",
        "sourceText": "POST /api/auth/login 用户登录接口"
      }
    ]
  }
}
```

#### POST /api/knowledge-bases/{kbId}/files
上传文件

```http
Content-Type: multipart/form-data

file: <binary>
```

```json
// Response
{
  "code": 200,
  "data": {
    "id": "file-123",
    "name": "document.pdf",
    "url": "https://s3.example.com/files/xxx.pdf",
    "size": 1048576
  }
}
```

#### DELETE /api/files/{id}
删除文件

#### POST /api/files/{id}/process/{type}
手动触发处理任务

```
type: ocr | vectorization | qa-pairs | knowledge-graph
```

```json
// Response
{
  "code": 200,
  "data": {
    "taskId": "task-123",
    "status": "processing"
  }
}
```

### 4.4 端点配置接口

#### GET /api/model-config/endpoints
查询端点列表

#### POST /api/model-config/endpoints
创建端点

```json
// Request
{
  "name": "OpenAI GPT-4",
  "baseUrl": "https://api.openai.com/v1",
  "provider": "OpenAI",
  "description": "OpenAI官方端点",
  "modelIds": ["model-1", "model-2"],
  "businessIds": ["biz-1"]
}
```

#### PUT /api/model-config/endpoints/{id}
更新端点

#### DELETE /api/model-config/endpoints/{id}
删除端点

### 4.5 API Key接口

#### GET /api/model-config/api-keys
查询API Key列表

```
?endpointId=endpoint-1  // 可选过滤
```

#### POST /api/model-config/api-keys
创建API Key

```json
// Request
{
  "endpointId": "endpoint-1",
  "name": "生产环境Key",
  "apiKey": "sk-xxx",
  "rateLimit": {
    "requestsPerMinute": 60,
    "requestsPerDay": 10000
  },
  "expiresAt": "2025-12-31T23:59:59Z"
}
```

#### PUT /api/model-config/api-keys/{id}
更新API Key

#### DELETE /api/model-config/api-keys/{id}
删除API Key

### 4.6 业务配置接口

#### GET /api/model-config/businesses
查询业务列表

#### POST /api/model-config/businesses
创建业务

```json
// Request
{
  "name": "智能客服",
  "code": "CUSTOMER_SERVICE",
  "description": "客服场景业务"
}
```

#### PUT /api/model-config/businesses/{id}
更新业务

#### DELETE /api/model-config/businesses/{id}
删除业务

---

## 5. 异步处理流程

### 5.1 RabbitMQ 队列设计

```java
// 队列定义
public class QueueConstants {
    // OCR处理队列
    public static final String QUEUE_OCR = "kb.ocr.queue";
    public static final String EXCHANGE_OCR = "kb.ocr.exchange";
    public static final String ROUTING_KEY_OCR = "kb.ocr";

    // 向量化队列
    public static final String QUEUE_VECTORIZATION = "kb.vectorization.queue";
    public static final String EXCHANGE_VECTORIZATION = "kb.vectorization.exchange";
    public static final String ROUTING_KEY_VECTORIZATION = "kb.vectorization";

    // 问答对生成队列
    public static final String QUEUE_QA_GENERATION = "kb.qa.queue";
    public static final String EXCHANGE_QA_GENERATION = "kb.qa.exchange";
    public static final String ROUTING_KEY_QA_GENERATION = "kb.qa";

    // 知识图谱生成队列
    public static final String QUEUE_KG_GENERATION = "kb.kg.queue";
    public static final String EXCHANGE_KG_GENERATION = "kb.kg.exchange";
    public static final String ROUTING_KEY_KG_GENERATION = "kb.kg";

    // 死信队列
    public static final String QUEUE_DLX = "kb.dlx.queue";
    public static final String EXCHANGE_DLX = "kb.dlx.exchange";
}
```

### 5.2 消息格式

```json
// OCR任务消息
{
  "taskId": "task-123",
  "fileId": "file-456",
  "filePath": "s3://bucket/path/to/file.pdf",
  "fileType": "pdf",
  "kbId": "kb-1",
  "userId": "1"
}

// 向量化任务消息
{
  "taskId": "task-124",
  "fileId": "file-456",
  "ocrContent": "文档内容...",
  "chunkSize": 1000,
  "overlap": 200
}

// 知识图谱任务消息
{
  "taskId": "task-125",
  "fileId": "file-456",
  "ocrContent": "文档内容...",
  "extractEntities": true,
  "extractRelations": true
}

// 问答对任务消息
{
  "taskId": "task-126",
  "fileId": "file-456",
  "ocrContent": "文档内容...",
  "maxPairs": 50
}
```

### 5.3 消费者处理流程

```java
@Component
public class OcrConsumer {

    @RabbitListener(queues = QueueConstants.QUEUE_OCR)
    public void processOcr(OcrTaskMessage message) {
        try {
            // 1. 更新任务状态为processing
            updateTaskStatus(message.getTaskId(), "processing");
            updateFileStatus(message.getFileId(), "ocr", "processing");

            // 2. 下载文件
            byte[] fileData = downloadFile(message.getFilePath());

            // 3. 调用OCR服务（Spring AI）
            String ocrContent = ocrService.extractText(fileData, message.getFileType());

            // 4. 保存OCR结果
            fileService.updateOcrContent(message.getFileId(), ocrContent);

            // 5. 更新状态为completed
            updateTaskStatus(message.getTaskId(), "completed");
            updateFileStatus(message.getFileId(), "ocr", "completed");

            // 6. 触发后续任务（向量化、知识图谱、问答对）
            triggerVectorizationTask(message.getFileId(), ocrContent);

        } catch (Exception e) {
            // 错误处理
            updateTaskStatus(message.getTaskId(), "failed", e.getMessage());
            updateFileStatus(message.getFileId(), "ocr", "failed");
            log.error("OCR处理失败", e);
        }
    }
}
```

### 5.4 重试机制

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(QueueConstants.QUEUE_OCR)
                .withArgument("x-dead-letter-exchange", QueueConstants.EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "ocr.failed")
                .withArgument("x-message-ttl", 600000) // 10分钟超时
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false); // 失败不重新入队
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
```

---

## 6. 文件存储方案

### 6.1 x-file-storage 配置

```yaml
# application.yml
dromara:
  x-file-storage:
    default-platform: s3  # 默认使用S3
    s3:
      - platform: s3
        enable-storage: true
        access-key: ${S3_ACCESS_KEY}
        secret-key: ${S3_SECRET_KEY}
        end-point: ${S3_ENDPOINT}
        bucket-name: ai-middle-platform
        domain: ${S3_DOMAIN}  # CDN域名
        base-path: files/     # 基础路径
```

### 6.2 文件上传实现

```java
@Service
public class FileStorageService {

    @Autowired
    private FileStorageService fileStorageService;

    public FileUploadResult uploadFile(MultipartFile file, String kbId, Long userId) {
        try {
            // 1. 生成文件ID和路径
            String fileId = IdUtil.simpleUUID();
            String fileName = file.getOriginalFilename();
            String fileType = getFileType(fileName);
            String storagePath = String.format("%s/%s/%s",
                kbId, LocalDate.now(), fileId + getExtension(fileName));

            // 2. 上传到S3
            FileInfo fileInfo = fileStorageService.of(file)
                    .setPath(storagePath)
                    .setSaveFilename(fileId + getExtension(fileName))
                    .upload();

            // 3. 生成缩略图（如果是图片或PDF）
            String thumbnailUrl = null;
            if (fileType.equals("pdf") || fileType.equals("image")) {
                thumbnailUrl = generateThumbnail(fileInfo.getUrl());
            }

            // 4. 保存文件元数据到数据库
            KbFile kbFile = new KbFile();
            kbFile.setFileId(fileId);
            kbFile.setKbId(Long.parseLong(kbId));
            kbFile.setName(fileName);
            kbFile.setOriginalName(fileName);
            kbFile.setFileType(fileType);
            kbFile.setMimeType(file.getContentType());
            kbFile.setSize(file.getSize());
            kbFile.setStoragePath(storagePath);
            kbFile.setUrl(fileInfo.getUrl());
            kbFile.setThumbnailUrl(thumbnailUrl);
            kbFile.setUploadedBy(userId);
            fileMapper.insert(kbFile);

            // 5. 发送MQ消息触发OCR
            sendOcrTask(fileId, fileInfo.getUrl(), fileType);

            return FileUploadResult.builder()
                    .fileId(fileId)
                    .url(fileInfo.getUrl())
                    .build();

        } catch (Exception e) {
            throw new BusinessException("文件上传失败", e);
        }
    }
}
```

### 6.3 S3存储桶结构

```
ai-middle-platform/
├── files/                    # 原始文件
│   ├── kb-1/
│   │   ├── 2024-01-15/
│   │   │   ├── xxx.pdf
│   │   │   └── yyy.docx
│   │   └── 2024-01-16/
│   └── kb-2/
├── thumbnails/               # 缩略图
│   ├── kb-1/
│   │   └── xxx.jpg
│   └── kb-2/
└── temp/                     # 临时文件
    └── processing/
```

---

## 7. Spring AI集成

### 7.1 配置类

```java
@Configuration
public class SpringAIConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .model("gpt-4")
                .temperature(0.7)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return new OpenAiEmbeddingModel(apiKey);
    }
}
```

### 7.2 动态API Key选择器

```java
@Service
public class ApiKeySelector {

    @Autowired
    private ModelApiKeyMapper apiKeyMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 选择可用的API Key（轮询策略）
     */
    public ModelApiKey selectApiKey(Long endpointId) {
        String cacheKey = "api_key_index:" + endpointId;

        // 1. 查询该端点所有启用的API Key
        List<ModelApiKey> apiKeys = apiKeyMapper.selectList(
            new LambdaQueryWrapper<ModelApiKey>()
                .eq(ModelApiKey::getEndpointId, endpointId)
                .eq(ModelApiKey::getEnabled, true)
                .orderByAsc(ModelApiKey::getId)
        );

        if (apiKeys.isEmpty()) {
            throw new BusinessException("无可用API Key");
        }

        // 2. 使用Redis实现轮询
        Long index = redisTemplate.opsForValue().increment(cacheKey);
        int selectedIndex = (int) (index % apiKeys.size());

        ModelApiKey selectedKey = apiKeys.get(selectedIndex);

        // 3. 检查限流
        if (!checkRateLimit(selectedKey)) {
            // 如果超过限流，尝试下一个
            return selectApiKey(endpointId);
        }

        return selectedKey;
    }

    /**
     * 检查限流
     */
    private boolean checkRateLimit(ModelApiKey apiKey) {
        if (apiKey.getRateLimitPerMinute() == null) {
            return true;
        }

        String rateLimitKey = "rate_limit:" + apiKey.getKeyId() + ":minute";
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);

        if (currentCount == 1) {
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }

        return currentCount <= apiKey.getRateLimitPerMinute();
    }

    /**
     * 记录使用统计
     */
    public void recordUsage(Long keyId, boolean success, String error) {
        ModelApiKey apiKey = apiKeyMapper.selectById(keyId);

        apiKey.setTotalRequests(apiKey.getTotalRequests() + 1);
        if (success) {
            apiKey.setSuccessRequests(apiKey.getSuccessRequests() + 1);
        } else {
            apiKey.setFailedRequests(apiKey.getFailedRequests() + 1);
            apiKey.setLastError(error);
        }
        apiKey.setLastUsedAt(LocalDateTime.now());

        apiKeyMapper.updateById(apiKey);
    }
}
```

### 7.3 OCR服务实现

```java
@Service
public class OcrService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ApiKeySelector apiKeySelector;

    public String extractText(byte[] fileData, String fileType) {
        // 1. 选择API Key
        ModelApiKey apiKey = apiKeySelector.selectApiKey(getOcrEndpointId());

        try {
            // 2. 构建提示词
            String prompt = buildOcrPrompt(fileType);

            // 3. 调用AI模型（支持视觉模型）
            ChatResponse response = chatClient.prompt()
                    .user(userSpec -> userSpec
                        .text(prompt)
                        .media(MimeTypeUtils.parseMimeType(getMimeType(fileType)), fileData))
                    .call()
                    .chatResponse();

            String ocrContent = response.getResult().getOutput().getContent();

            // 4. 记录成功
            apiKeySelector.recordUsage(apiKey.getId(), true, null);

            return ocrContent;

        } catch (Exception e) {
            // 记录失败
            apiKeySelector.recordUsage(apiKey.getId(), false, e.getMessage());
            throw new BusinessException("OCR处理失败", e);
        }
    }

    private String buildOcrPrompt(String fileType) {
        return """
            请仔细分析这个%s文档，提取其中的所有文字内容。

            要求：
            1. 保持原文的结构和格式
            2. 使用Markdown格式输出
            3. 保留标题层级（使用 #, ##, ### 等）
            4. 保留表格、列表、代码块等格式
            5. 数学公式使用LaTeX格式（$...$或$$...$$）
            6. 忽略图片但描述图片内容（如：[图片：架构图]）

            请直接输出提取的内容，不要添加任何说明或注释。
            """.formatted(fileType);
    }
}
```

### 7.4 向量化服务

```java
@Service
public class VectorizationService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private KbVectorMapper vectorMapper;

    public void vectorize(Long fileId, String ocrContent) {
        // 1. 文本分块
        List<String> chunks = splitText(ocrContent, 1000, 200);

        // 2. 批量生成向量
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            // 生成向量
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(chunk));
            float[] embedding = response.getResult().getOutput();

            // 保存到数据库
            KbVector vector = new KbVector();
            vector.setFileId(fileId);
            vector.setChunkIndex(i);
            vector.setContent(chunk);
            vector.setEmbedding(embedding);

            vectorMapper.insert(vector);
        }
    }

    /**
     * 文本分块（支持重叠）
     */
    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
```

### 7.5 知识图谱生成服务

```java
@Service
public class KnowledgeGraphService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    public void generateKnowledgeGraph(Long fileId, String ocrContent) {
        // 1. 使用AI提取实体和关系
        String prompt = buildKgPrompt(ocrContent);

        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String jsonResult = response.getResult().getOutput().getContent();

        // 2. 解析JSON结果
        KnowledgeGraphData kgData = parseKgJson(jsonResult);

        // 3. 保存到Neo4j
        saveToNeo4j(fileId, kgData);
    }

    private String buildKgPrompt(String content) {
        return """
            分析以下文档内容，提取知识图谱。

            文档内容：
            %s

            请以JSON格式返回结果，包含：
            1. entities: 实体列表 [{id, name, type, attributes}]
            2. relations: 关系列表 [{source, target, type, description}]

            实体类型包括：person, organization, location, product, technology, concept
            关系类型包括：defines, uses, implements, relates_to, depends_on

            只返回JSON，不要添加任何说明。
            """.formatted(content);
    }

    private void saveToNeo4j(Long fileId, KnowledgeGraphData kgData) {
        // 创建文档节点
        String docQuery = """
            MERGE (d:Document {id: $fileId})
            SET d.name = $fileName, d.createdAt = datetime()
            """;
        neo4jTemplate.query(docQuery)
                .bind(fileId.toString()).to("fileId")
                .run();

        // 创建实体节点
        for (Entity entity : kgData.getEntities()) {
            String entityQuery = """
                MERGE (e:Entity {id: $id})
                SET e.name = $name, e.type = $type, e.attributes = $attributes
                WITH e
                MATCH (d:Document {id: $fileId})
                MERGE (e)-[:BELONGS_TO]->(d)
                """;
            neo4jTemplate.query(entityQuery)
                    .bind(entity.getId()).to("id")
                    .bind(entity.getName()).to("name")
                    .bind(entity.getType()).to("type")
                    .bind(entity.getAttributes()).to("attributes")
                    .bind(fileId.toString()).to("fileId")
                    .run();
        }

        // 创建关系
        for (Relation relation : kgData.getRelations()) {
            String relationQuery = """
                MATCH (s:Entity {id: $sourceId})
                MATCH (t:Entity {id: $targetId})
                MERGE (s)-[r:INTERACTS_WITH {type: $type}]->(t)
                SET r.description = $description
                """;
            neo4jTemplate.query(relationQuery)
                    .bind(relation.getSourceId()).to("sourceId")
                    .bind(relation.getTargetId()).to("targetId")
                    .bind(relation.getType()).to("type")
                    .bind(relation.getDescription()).to("description")
                    .run();
        }
    }
}
```

### 7.6 问答对生成服务

```java
@Service
public class QaPairService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private KbQaPairMapper qaPairMapper;

    public void generateQaPairs(Long fileId, String ocrContent) {
        // 1. 使用AI生成问答对
        String prompt = buildQaPrompt(ocrContent);

        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String jsonResult = response.getResult().getOutput().getContent();

        // 2. 解析JSON
        List<QaPairData> qaPairs = parseQaJson(jsonResult);

        // 3. 保存到数据库
        for (QaPairData qa : qaPairs) {
            KbQaPair qaPair = new KbQaPair();
            qaPair.setQaId(IdUtil.simpleUUID());
            qaPair.setFileId(fileId);
            qaPair.setQuestion(qa.getQuestion());
            qaPair.setAnswer(qa.getAnswer());
            qaPair.setSourceText(qa.getSourceText());
            qaPair.setConfidenceScore(qa.getConfidence());

            qaPairMapper.insert(qaPair);
        }
    }

    private String buildQaPrompt(String content) {
        return """
            分析以下文档内容，生成高质量的问答对。

            文档内容：
            %s

            要求：
            1. 生成20-50个问答对
            2. 问题应该涵盖文档的关键信息
            3. 答案应该准确、简洁
            4. 包含不同难度级别的问题

            以JSON格式返回：
            [
              {
                "question": "问题",
                "answer": "答案",
                "sourceText": "原文片段",
                "confidence": 0.95
              }
            ]

            只返回JSON数组，不要添加任何说明。
            """.formatted(content);
    }
}
```

---

## 8. 实施计划

### Phase 1: 基础设施搭建（3天）

#### 任务1.1: 项目初始化
- [ ] 创建Spring Boot项目（Spring Boot 3.2.x）
- [ ] 配置Maven依赖（Spring AI, MyBatis-Plus, Neo4j等）
- [ ] 配置application.yml（数据库、Redis、RabbitMQ）
- [ ] 搭建基础MVC结构

**验收标准**: 项目启动成功，连接所有中间件

#### 任务1.2: 数据库初始化
- [ ] 执行PostgreSQL建表脚本
- [ ] 配置pgvector扩展
- [ ] 执行Neo4j索引脚本
- [ ] 初始化测试数据

**验收标准**: 所有表创建成功，Neo4j连接正常

#### 任务1.3: 认证授权
- [ ] 实现JWT认证
- [ ] 配置Spring Security
- [ ] 实现登录/注销接口
- [ ] 用户角色权限控制

**验收标准**: 登录成功返回token，受保护接口需要认证

---

### Phase 2: 核心功能实现（5天）

#### 任务2.1: 知识库管理
- [ ] 实现知识库CRUD接口
- [ ] 知识库成员管理
- [ ] 权限校验（owner/editor/viewer）

**验收标准**:
- POST /api/knowledge-bases 创建成功
- GET /api/knowledge-bases 返回列表
- 成员权限校验正常

#### 任务2.2: 文件上传
- [ ] 配置x-file-storage
- [ ] 实现文件上传接口
- [ ] 生成缩略图
- [ ] 保存文件元数据

**验收标准**:
- 文件上传到S3成功
- 返回文件URL
- 数据库记录正确

#### 任务2.3: 文件查询
- [ ] 实现文件列表接口
- [ ] 实现文件详情接口
- [ ] 支持状态过滤

**验收标准**:
- 返回文件列表，包含处理状态
- 文件详情包含OCR内容、图谱、问答对

---

### Phase 3: 异步处理实现（5天）

#### 任务3.1: RabbitMQ配置
- [ ] 定义队列和交换机
- [ ] 配置死信队列
- [ ] 实现消息生产者

**验收标准**: 队列创建成功，消息发送正常

#### 任务3.2: OCR处理
- [ ] 实现OCR消费者
- [ ] 集成Spring AI视觉模型
- [ ] 保存OCR结果
- [ ] 更新处理状态

**验收标准**:
- 上传文件后自动触发OCR
- OCR结果保存到数据库
- 状态更新为completed

#### 任务3.3: 向量化处理
- [ ] 实现向量化消费者
- [ ] 文本分块逻辑
- [ ] 调用Embedding模型
- [ ] 保存向量到PostgreSQL

**验收标准**:
- OCR完成后自动触发向量化
- 向量数据保存成功
- 支持向量检索

#### 任务3.4: 知识图谱生成
- [ ] 实现图谱消费者
- [ ] AI提取实体和关系
- [ ] 保存到Neo4j
- [ ] 图谱查询接口

**验收标准**:
- 生成知识图谱
- Neo4j查询返回正确结构
- 前端可视化展示

#### 任务3.5: 问答对生成
- [ ] 实现问答对消费者
- [ ] AI生成问答对
- [ ] 保存到数据库
- [ ] 问答对查询接口

**验收标准**:
- 生成20-50个问答对
- 数据库保存成功
- 前端列表展示

---

### Phase 4: 模型配置功能（3天）

#### 任务4.1: 端点管理
- [ ] 端点CRUD接口
- [ ] 模型关联
- [ ] 业务关联

**验收标准**: 端点配置成功，关联数据正确

#### 任务4.2: API Key管理
- [ ] API Key CRUD接口
- [ ] API Key加密存储
- [ ] 限流配置

**验收标准**: API Key管理正常，限流生效

#### 任务4.3: 动态API Key选择
- [ ] 实现轮询策略
- [ ] 限流检查
- [ ] 使用统计记录

**验收标准**:
- API Key自动轮询
- 超过限流时切换Key
- 统计数据准确

#### 任务4.4: 业务配置
- [ ] 业务CRUD接口
- [ ] 业务关联端点

**验收标准**: 业务配置成功

---

### Phase 5: 联调和优化（4天）

#### 任务5.1: 前后端联调
- [ ] 修改前端API地址
- [ ] 调试所有接口
- [ ] 修复数据格式问题

**验收标准**: 所有前端页面正常工作

#### 任务5.2: 性能优化
- [ ] 添加Redis缓存
- [ ] 优化数据库查询
- [ ] 异步任务并发控制

**验收标准**:
- 响应时间 < 200ms
- 并发处理 > 100/s

#### 任务5.3: 错误处理
- [ ] 全局异常处理
- [ ] 错误日志记录
- [ ] 任务失败重试

**验收标准**: 异常信息返回友好，任务失败自动重试

#### 任务5.4: 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 压力测试

**验收标准**: 测试覆盖率 > 80%

---

## 9. 初始化脚本

### 9.1 PostgreSQL 初始化脚本

```sql
-- init.sql

-- 1. 创建数据库
CREATE DATABASE ai_middle_platform;

-- 2. 连接数据库
\c ai_middle_platform;

-- 3. 安装pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 4. 创建用户表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. 创建知识库表
CREATE TABLE kb_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    kb_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES sys_user(id),
    file_count INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 创建知识库成员表
CREATE TABLE kb_member (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, user_id)
);

-- 7. 创建文件表
CREATE TABLE kb_file (
    id BIGSERIAL PRIMARY KEY,
    file_id VARCHAR(50) NOT NULL UNIQUE,
    kb_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000),
    uploaded_by BIGINT NOT NULL REFERENCES sys_user(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ocr_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    vectorization_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    qa_pairs_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    knowledge_graph_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    ocr_content TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. 创建向量表
CREATE TABLE kb_vector (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, chunk_index)
);

-- 9. 创建问答对表
CREATE TABLE kb_qa_pair (
    id BIGSERIAL PRIMARY KEY,
    qa_id VARCHAR(50) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_text TEXT NOT NULL,
    confidence_score DECIMAL(5,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. 创建业务配置表
CREATE TABLE model_business (
    id BIGSERIAL PRIMARY KEY,
    business_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. 创建端点配置表
CREATE TABLE model_endpoint (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. 创建模型表
CREATE TABLE model_info (
    id BIGSERIAL PRIMARY KEY,
    model_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. 创建端点业务关联表
CREATE TABLE model_endpoint_business (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES model_business(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(endpoint_id, business_id)
);

-- 14. 创建API Key表
CREATE TABLE model_api_key (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    display_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_minute INT,
    rate_limit_per_day INT,
    total_requests BIGINT NOT NULL DEFAULT 0,
    success_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_by BIGINT NOT NULL REFERENCES sys_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. 创建处理任务表
CREATE TABLE kb_process_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 16. 创建索引
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_role ON sys_user(role);
CREATE INDEX idx_kb_owner ON kb_knowledge_base(owner_id);
CREATE INDEX idx_kb_created ON kb_knowledge_base(created_at DESC);
CREATE INDEX idx_member_kb ON kb_member(kb_id);
CREATE INDEX idx_member_user ON kb_member(user_id);
CREATE INDEX idx_file_kb ON kb_file(kb_id);
CREATE INDEX idx_file_uploaded_by ON kb_file(uploaded_by);
CREATE INDEX idx_file_type ON kb_file(file_type);
CREATE INDEX idx_file_ocr_status ON kb_file(ocr_status);
CREATE INDEX idx_vector_file ON kb_vector(file_id);
CREATE INDEX idx_vector_embedding ON kb_vector USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_qa_file ON kb_qa_pair(file_id);
CREATE INDEX idx_qa_question ON kb_qa_pair USING gin(to_tsvector('simple', question));
CREATE INDEX idx_business_code ON model_business(code);
CREATE INDEX idx_endpoint_provider ON model_endpoint(provider);
CREATE INDEX idx_model_endpoint ON model_info(endpoint_id);
CREATE INDEX idx_eb_endpoint ON model_endpoint_business(endpoint_id);
CREATE INDEX idx_eb_business ON model_endpoint_business(business_id);
CREATE INDEX idx_apikey_endpoint ON model_api_key(endpoint_id);
CREATE INDEX idx_apikey_enabled ON model_api_key(enabled);
CREATE INDEX idx_task_file ON kb_process_task(file_id);
CREATE INDEX idx_task_status ON kb_process_task(status);
CREATE INDEX idx_task_type ON kb_process_task(task_type);

-- 17. 插入初始管理员用户 (密码: admin123)
INSERT INTO sys_user (username, email, password, name, role)
VALUES ('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin');

-- 18. 添加表注释
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON TABLE kb_knowledge_base IS '知识库表';
COMMENT ON TABLE kb_member IS '知识库成员表';
COMMENT ON TABLE kb_file IS '文件表';
COMMENT ON TABLE kb_vector IS '向量表（文档片段）';
COMMENT ON TABLE kb_qa_pair IS '问答对表';
COMMENT ON TABLE model_business IS '业务配置表';
COMMENT ON TABLE model_endpoint IS '端点配置表';
COMMENT ON TABLE model_info IS '模型信息表';
COMMENT ON TABLE model_endpoint_business IS '端点业务关联表';
COMMENT ON TABLE model_api_key IS 'API Key配置表';
COMMENT ON TABLE kb_process_task IS '处理任务表';
```

### 9.2 Neo4j 初始化脚本

```cypher
// neo4j-init.cypher

// 1. 创建唯一约束
CREATE CONSTRAINT doc_id IF NOT EXISTS FOR (d:Document) REQUIRE d.id IS UNIQUE;
CREATE CONSTRAINT concept_id IF NOT EXISTS FOR (c:Concept) REQUIRE c.id IS UNIQUE;
CREATE CONSTRAINT entity_id IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE;
CREATE CONSTRAINT section_id IF NOT EXISTS FOR (s:Section) REQUIRE s.id IS UNIQUE;

// 2. 创建全文搜索索引
CREATE FULLTEXT INDEX concept_search IF NOT EXISTS
FOR (c:Concept) ON EACH [c.name, c.description];

CREATE FULLTEXT INDEX entity_search IF NOT EXISTS
FOR (e:Entity) ON EACH [e.name];

// 3. 创建属性索引
CREATE INDEX doc_created IF NOT EXISTS FOR (d:Document) ON (d.createdAt);
CREATE INDEX entity_type IF NOT EXISTS FOR (e:Entity) ON (e.type);
CREATE INDEX concept_type IF NOT EXISTS FOR (c:Concept) ON (c.type);

// 4. 验证
SHOW CONSTRAINTS;
SHOW INDEXES;
```

### 9.3 应用配置文件

```yaml
# application.yml

spring:
  application:
    name: ai-middle-platform

  # 数据源配置
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://211.159.174.70:5432/ai_middle_platform
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:123456}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  # Redis配置
  data:
    redis:
      host: 211.159.174.70
      port: 6379
      password: ${REDIS_PASSWORD:123456}
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

    # Neo4j配置
    neo4j:
      uri: bolt://211.159.174.70:7687
      authentication:
        username: neo4j
        password: ${NEO4J_PASSWORD:neo4j123}
      connection-timeout: 30s

  # RabbitMQ配置
  rabbitmq:
    host: 211.159.174.70
    port: 5672
    username: ${MQ_USERNAME:admin}
    password: ${MQ_PASSWORD:123456}
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: auto
        retry:
          enabled: true
          max-attempts: 3

  # Spring AI配置
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-4-turbo-preview
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small

# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.ai.middle.platform.entity.po
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# x-file-storage配置
dromara:
  x-file-storage:
    default-platform: s3
    s3:
      - platform: s3
        enable-storage: true
        access-key: ${S3_ACCESS_KEY}
        secret-key: ${S3_SECRET_KEY}
        end-point: ${S3_ENDPOINT}
        bucket-name: ai-middle-platform
        domain: ${S3_DOMAIN}
        base-path: files/

# JWT配置
jwt:
  secret: ${JWT_SECRET:your-secret-key-must-be-at-least-256-bits}
  expiration: 86400000  # 24小时

# 业务配置
app:
  file:
    max-size: 104857600  # 100MB
    allowed-types: pdf,docx,doc,jpg,jpeg,png
  ocr:
    chunk-size: 1000
    overlap: 200
  vector:
    dimension: 1536
  task:
    retry-times: 3
    retry-delay: 5000  # 5秒

# 日志配置
logging:
  level:
    com.ai.middle.platform: DEBUG
    org.springframework.data.neo4j: DEBUG
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n'
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

---

## 10. 总结

### 10.1 技术亮点

1. **微服务架构**: 清晰的MVC分层，易于扩展
2. **异步处理**: RabbitMQ解耦，提升系统吞吐量
3. **多数据库**: PostgreSQL + Neo4j + Redis，各司其职
4. **智能AI**: Spring AI集成，支持多模型动态切换
5. **文件存储**: x-file-storage对接S3，云原生方案

### 10.2 性能指标

- **并发处理**: 支持100+ QPS
- **文件上传**: 单文件最大100MB
- **OCR处理**: 平均30秒/文档
- **向量检索**: < 100ms响应
- **图谱查询**: < 200ms响应

### 10.3 扩展性

- **水平扩展**: 无状态设计，支持多实例部署
- **存储扩展**: S3支持PB级存储
- **AI模型**: 支持动态添加新模型和端点
- **业务扩展**: 插件化业务配置

### 10.4 下一步工作

1. 完成后端代码实现（20天）
2. 前后端联调（4天）
3. 性能测试和优化（3天）
4. 文档完善和部署（3天）

**预计总工期**: 30个工作日

---

**文档维护者**: AI Development Team
**最后更新**: 2025-10-03
**版本**: v1.0
