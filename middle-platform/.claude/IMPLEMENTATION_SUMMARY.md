# AI知识库管理系统 - 后端实现总结

> **项目名称**: AI Middle Platform - Knowledge Base Management System
> **完成日期**: 2025-10-03
> **技术栈**: Spring Boot 3.2 + Spring AI + Neo4j + PostgreSQL + RabbitMQ + Redis
> **实施方式**: Claude Code + Codex MCP

---

## 📋 项目概述

AI知识库管理系统是一个基于AI的智能文档处理平台，支持文档上传、OCR识别、文本向量化、知识图谱生成和问答对提取等功能。

### 核心功能

1. **知识库管理**: 创建、查询、删除知识库，支持成员协作
2. **文件管理**: 上传文档（PDF/Word/图片），文件元数据管理
3. **智能处理**: OCR识别、文本向量化、知识图谱生成、问答对提取
4. **异步处理**: 基于RabbitMQ的异步任务处理
5. **认证授权**: JWT Token认证

---

## ✅ 已完成的工作

### 1. 项目规划 ✓

**文档位置**: `.claude/plan/backend-development.md`

- 完整的技术架构设计
- 详细的数据库设计（PostgreSQL + Neo4j）
- RESTful API接口设计
- 异步处理流程设计
- 实施计划（5个阶段）

### 2. 数据库设计 ✓

#### PostgreSQL (12个表)

**初始化脚本**: `src/main/resources/sql/init.sql`

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_user | 用户表 | id, username, email, password, role |
| kb_knowledge_base | 知识库表 | id, kb_id, name, owner_id, file_count |
| kb_member | 知识库成员表 | id, kb_id, user_id, role |
| kb_file | 文件表 | id, file_id, kb_id, name, url, statuses |
| kb_vector | 向量表 | id, file_id, chunk_index, embedding |
| kb_qa_pair | 问答对表 | id, qa_id, file_id, question, answer |
| kb_process_task | 处理任务表 | id, task_id, file_id, task_type, status |
| model_business | 业务配置表 | id, business_id, name, code |
| model_endpoint | 端点配置表 | id, endpoint_id, name, base_url, provider |
| model_info | 模型信息表 | id, model_id, endpoint_id, name |
| model_endpoint_business | 端点业务关联表 | id, endpoint_id, business_id |
| model_api_key | API Key表 | id, key_id, endpoint_id, api_key, rate_limit |

#### Neo4j (图数据库)

**初始化脚本**: `src/main/resources/sql/neo4j-init.cypher`

- **节点类型**: Document, Entity, Concept, Section
- **关系类型**: BELONGS_TO, CONTAINS, MENTIONS, RELATED_TO, INTERACTS_WITH, DEFINES, PARENT_OF
- **索引**: 唯一约束 + 全文搜索索引

### 3. 项目配置 ✓

#### Maven依赖 (`pom.xml`)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```

**核心依赖**:
- Spring Boot Web/Security/Data JPA
- Spring AI (OpenAI)
- MyBatis-Plus 3.5.5
- PostgreSQL + Neo4j + Redis + RabbitMQ
- x-file-storage (S3)
- JWT (jjwt 0.12.3)
- Lombok + Hutool

#### 应用配置 (`application.yml`)

- 数据源配置（PostgreSQL + Hikari连接池）
- Redis配置
- Neo4j配置
- RabbitMQ配置（4个业务队列 + 死信队列）
- Spring AI配置
- x-file-storage配置
- MyBatis-Plus配置
- JWT配置

### 4. 项目结构 ✓

```
src/main/java/com/ai/middle/platform/
├── config/                          # 配置类 (5个)
│   ├── MyBatisPlusConfig.java       # MyBatis-Plus配置
│   ├── RedisConfig.java             # Redis配置
│   ├── SecurityConfig.java          # Spring Security + JWT
│   ├── RabbitMQConfig.java          # RabbitMQ配置
│   └── WebMvcConfig.java            # Web MVC配置
├── controller/                      # 控制器 (3个)
│   ├── AuthController.java          # 认证控制器
│   ├── KnowledgeBaseController.java # 知识库控制器
│   └── FileController.java          # 文件控制器
├── service/                         # 服务接口 (4个)
│   ├── AuthService.java
│   ├── KnowledgeBaseService.java
│   ├── FileService.java
│   └── AIProcessService.java
├── service/impl/                    # 服务实现 (4个)
│   ├── AuthServiceImpl.java
│   ├── KnowledgeBaseServiceImpl.java
│   ├── FileServiceImpl.java
│   └── AIProcessServiceImpl.java    # AI处理服务（模拟实现）
├── repository/
│   ├── mapper/                      # MyBatis-Plus Mapper (12个)
│   │   ├── SysUserMapper.java
│   │   ├── KbKnowledgeBaseMapper.java
│   │   ├── KbFileMapper.java
│   │   └── ...
│   └── neo4j/                       # Neo4j Repository (4个)
│       ├── DocumentNodeRepository.java
│       ├── EntityNodeRepository.java
│       ├── ConceptNodeRepository.java
│       └── SectionNodeRepository.java
├── entity/
│   ├── po/                          # PostgreSQL实体 (12个)
│   │   ├── SysUser.java
│   │   ├── KbKnowledgeBase.java
│   │   ├── KbFile.java
│   │   └── ...
│   └── graph/                       # Neo4j图实体 (4个)
│       ├── DocumentNode.java
│       ├── EntityNode.java
│       ├── ConceptNode.java
│       └── SectionNode.java
├── dto/
│   ├── request/                     # 请求DTO (2个)
│   │   ├── LoginRequest.java
│   │   └── KnowledgeBaseRequest.java
│   └── response/                    # 响应DTO (9个)
│       ├── LoginResponse.java
│       ├── UserDTO.java
│       ├── KnowledgeBaseDTO.java
│       ├── MemberDTO.java
│       ├── FileDTO.java
│       ├── FileDetailDTO.java
│       ├── FileStatusesDTO.java
│       ├── KnowledgeGraphDTO.java
│       └── QaPairDTO.java
├── mq/
│   ├── message/                     # 消息类 (4个)
│   │   ├── OcrTaskMessage.java
│   │   ├── VectorizationTaskMessage.java
│   │   ├── QaGenerationTaskMessage.java
│   │   └── KgGenerationTaskMessage.java
│   └── consumer/                    # 消费者 (4个)
│       ├── OcrConsumer.java
│       ├── VectorizationConsumer.java
│       ├── QaGenerationConsumer.java
│       └── KgGenerationConsumer.java
├── common/
│   ├── enums/                       # 枚举 (4个)
│   │   ├── ProcessingStatus.java
│   │   ├── FileType.java
│   │   ├── UserRole.java
│   │   └── MemberRole.java
│   ├── exception/                   # 异常 (2个)
│   │   ├── GlobalExceptionHandler.java
│   │   └── BusinessException.java
│   ├── constant/                    # 常量 (1个)
│   │   └── ApiConstants.java
│   ├── util/                        # 工具类 (2个)
│   │   ├── JwtUtil.java
│   │   └── IdGenerator.java
│   └── result/                      # 返回结果 (1个)
│       └── Result.java
└── MiddlePlatformApplication.java   # 启动类
```

### 5. 核心功能实现 ✓

#### 认证模块

- ✅ 用户登录（邮箱 + 密码）
- ✅ JWT Token生成和验证
- ✅ 密码加密（BCrypt）
- ✅ 用户登出

**接口**:
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出

#### 知识库管理模块

- ✅ 知识库CRUD操作
- ✅ 知识库成员管理
- ✅ 权限控制（owner/editor/viewer）

**接口**:
- `GET /api/knowledge-bases` - 查询知识库列表
- `GET /api/knowledge-bases/{id}` - 查询知识库详情
- `POST /api/knowledge-bases` - 创建知识库
- `DELETE /api/knowledge-bases/{id}` - 删除知识库

#### 文件管理模块

- ✅ 文件上传（支持PDF/Word/图片）
- ✅ 文件列表查询
- ✅ 文件详情查询（含OCR内容、知识图谱、问答对）
- ✅ 文件删除
- ✅ 手动触发处理任务

**接口**:
- `GET /api/knowledge-bases/{kbId}/files` - 查询文件列表
- `GET /api/files/{id}` - 查询文件详情
- `POST /api/knowledge-bases/{kbId}/files` - 上传文件
- `DELETE /api/files/{id}` - 删除文件
- `POST /api/files/{id}/process/{type}` - 触发处理任务

#### 异步处理模块

- ✅ OCR文字识别（消费者 + 模拟实现）
- ✅ 文本向量化（消费者 + 模拟实现）
- ✅ 问答对生成（消费者 + 模拟实现）
- ✅ 知识图谱生成（消费者 + 模拟实现）
- ✅ 处理状态管理（pending/processing/completed/failed）
- ✅ 任务链触发（OCR → 向量化 → QA生成 + KG生成）

**消息队列**:
- `kb.ocr.queue` - OCR处理队列
- `kb.vectorization.queue` - 向量化处理队列
- `kb.qa.queue` - 问答对生成队列
- `kb.kg.queue` - 知识图谱生成队列
- `kb.dlx.queue` - 死信队列

### 6. 技术亮点 ✓

1. **微服务架构**: 清晰的MVC分层，易于扩展
2. **异步处理**: RabbitMQ解耦，提升系统吞吐量
3. **多数据库**: PostgreSQL + Neo4j + Redis，各司其职
4. **智能AI**: Spring AI集成（模拟实现，可替换为真实AI）
5. **文件存储**: x-file-storage对接S3（模拟实现）
6. **安全认证**: JWT + Spring Security
7. **统一返回**: Result统一封装
8. **全局异常**: GlobalExceptionHandler统一处理

---

## 📊 统计数据

### 代码统计

| 类型 | 数量 | 说明 |
|------|------|------|
| **配置类** | 5 | Spring配置 |
| **控制器** | 3 | REST API |
| **服务接口** | 4 | 业务逻辑接口 |
| **服务实现** | 4 | 业务逻辑实现 |
| **PostgreSQL实体** | 12 | 数据库实体 |
| **Neo4j图实体** | 4 | 图数据库节点 |
| **Mapper接口** | 12 | MyBatis-Plus |
| **Neo4j Repository** | 4 | 图数据库查询 |
| **DTO类** | 11 | 数据传输对象 |
| **消息类** | 4 | RabbitMQ消息 |
| **消费者** | 4 | MQ消费者 |
| **枚举类** | 4 | 枚举定义 |
| **工具类** | 4 | 通用工具 |
| **总计** | **75个类** | - |

### API接口统计

| 模块 | 接口数 |
|------|-------|
| 认证 | 2 |
| 知识库 | 4 |
| 文件 | 5 |
| **总计** | **11个接口** |

---

## 🔧 技术架构

### 后端架构

```
┌─────────────────────────────────────────────┐
│         前端 (Next.js + React)               │
│              HTTP/REST API                  │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│     Spring Boot 3.2 (API Gateway)           │
│  ┌────────────┐  ┌────────────┐            │
│  │ Controller │  │  Service   │            │
│  └────────────┘  └────────────┘            │
│  ┌────────────┐  ┌────────────┐            │
│  │   Mapper   │  │ Repository │            │
│  └────────────┘  └────────────┘            │
└────────────────┬────────────────────────────┘
                 │
    ┌────────────┼────────────┐
    ▼            ▼            ▼
┌─────────┐  ┌────────┐  ┌─────────┐
│PostgreSQL│  │ Neo4j  │  │  Redis  │
└─────────┘  └────────┘  └─────────┘
                 │
                 ▼
         ┌─────────────┐
         │  RabbitMQ   │
         │ (异步处理)   │
         └─────────────┘
                 │
                 ▼
         ┌─────────────┐
         │  Spring AI  │
         │ (AI处理)     │
         └─────────────┘
```

### 异步处理流程

```
文件上传 → 保存元数据 → 发送OCR消息
                              │
                              ▼
                         OCR消费者
                              │
                              ▼
                     更新OCR内容和状态
                              │
                              ▼
                   发送向量化消息
                              │
                              ▼
                       向量化消费者
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              QA生成消息          KG生成消息
                    │                   │
                    ▼                   ▼
              QA消费者            KG消费者
                    │                   │
                    ▼                   ▼
            保存问答对          保存知识图谱
```

---

## ⚠️ 待完善事项

### 1. AI功能实现

当前AI处理服务使用**模拟实现**，需要替换为真实的Spring AI调用：

**文件位置**: `AIProcessServiceImpl.java`

#### 需要实现的功能

1. **OCR识别** (`performOcr`)
   ```java
   // TODO: 调用Spring AI的视觉模型进行OCR
   // 参考：ChatClient + multimodal input
   ```

2. **文本向量化** (`performVectorization`)
   ```java
   // TODO: 调用OpenAI Embedding API
   // 参考：EmbeddingModel.embedForResponse()
   ```

3. **问答对生成** (`generateQaPairs`)
   ```java
   // TODO: 调用ChatModel生成问答对
   // 参考：ChatClient + structured prompt
   ```

4. **知识图谱生成** (`generateKnowledgeGraph`)
   ```java
   // TODO: 调用ChatModel提取实体和关系
   // 参考：ChatClient + JSON output parsing
   ```

### 2. 文件存储集成

当前文件上传使用**模拟实现**，需要集成x-file-storage：

**文件位置**: `FileServiceImpl.java`

```java
// TODO: 实际上传到S3
@Autowired
private FileStorageService fileStorageService;

FileInfo fileInfo = fileStorageService.of(file)
        .setPath(storagePath)
        .upload();
```

### 3. 安全性增强

1. **获取当前用户**
   ```java
   // TODO: 从SecurityContext获取当前用户
   // 替换所有硬编码的 userId = 1L
   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
   ```

2. **Token黑名单**
   ```java
   // TODO: 实现Token黑名单（使用Redis）
   // 在AuthServiceImpl.logout()中实现
   ```

3. **权限控制**
   ```java
   // TODO: 实现细粒度权限控制
   // 使用@PreAuthorize注解
   ```

### 4. 模型配置功能

以下Controller和Service尚未实现：

- [ ] EndpointController - 端点配置管理
- [ ] ApiKeyController - API Key管理
- [ ] BusinessController - 业务配置管理

### 5. 前后端联调

- [ ] 修改前端API地址
- [ ] 测试所有接口
- [ ] 处理CORS问题
- [ ] 统一错误处理
- [ ] 数据格式对齐

---

## 🚀 快速启动

### 1. 环境准备

#### 必需的服务

```bash
# PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=123456 \
  -p 5432:5432 postgres:15

# Neo4j
docker run -d --name neo4j \
  -e NEO4J_AUTH=neo4j/neo4j123 \
  -p 7474:7474 -p 7687:7687 neo4j:5

# Redis
docker run -d --name redis \
  -p 6379:6379 redis:7

# RabbitMQ
docker run -d --name rabbitmq \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

#### 初始化数据库

```bash
# PostgreSQL
psql -h 211.159.174.70 -U postgres < src/main/resources/sql/init.sql

# Neo4j
cypher-shell -a bolt://211.159.174.70:7687 -u neo4j -p neo4j123 \
  < src/main/resources/sql/neo4j-init.cypher
```

### 2. 配置环境变量

创建 `.env` 文件：

```properties
# 数据库
DB_USERNAME=postgres
DB_PASSWORD=123456

# Redis
REDIS_PASSWORD=123456

# Neo4j
NEO4J_PASSWORD=neo4j123

# RabbitMQ
MQ_USERNAME=admin
MQ_PASSWORD=123456

# OpenAI
OPENAI_API_KEY=sk-xxx
OPENAI_BASE_URL=https://api.openai.com

# S3
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
S3_ENDPOINT=your-endpoint
S3_BUCKET=ai-middle-platform
S3_DOMAIN=your-domain

# JWT
JWT_SECRET=your-secret-key-must-be-at-least-256-bits-long
```

### 3. 启动应用

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/middle-platform-1.0-SNAPSHOT.jar

# 或使用Maven
mvn spring-boot:run
```

### 4. 访问应用

- **API**: http://localhost:8080/api
- **RabbitMQ管理**: http://localhost:15672 (admin/123456)
- **Neo4j浏览器**: http://localhost:7474 (neo4j/neo4j123)

---

## 📝 API使用示例

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "name": "系统管理员",
      "role": "admin"
    }
  }
}
```

### 创建知识库

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "name": "技术文档库",
    "description": "存储技术文档"
  }'
```

### 上传文件

```bash
curl -X POST http://localhost:8080/api/knowledge-bases/{kbId}/files \
  -H "Authorization: Bearer {token}" \
  -F "file=@document.pdf"
```

### 查询文件详情

```bash
curl -X GET http://localhost:8080/api/files/{fileId} \
  -H "Authorization: Bearer {token}"
```

---

## 🎯 下一步工作

### 优先级1：核心功能完善

1. **集成真实AI服务**
   - 替换AIProcessServiceImpl中的模拟实现
   - 配置Spring AI的ChatClient和EmbeddingModel
   - 测试OCR、向量化、QA生成、KG生成

2. **集成文件存储**
   - 配置x-file-storage对接S3
   - 实现文件上传/下载
   - 实现缩略图生成

3. **完善安全功能**
   - 实现SecurityContext用户获取
   - 实现Token黑名单
   - 添加权限控制注解

### 优先级2：功能扩展

1. **实现模型配置功能**
   - EndpointController
   - ApiKeyController
   - BusinessController

2. **添加Dashboard功能**
   - 系统统计
   - 使用分析
   - 性能监控

3. **完善用户管理**
   - 用户注册
   - 密码重置
   - 用户权限管理

### 优先级3：性能优化

1. **添加缓存**
   - Redis缓存热点数据
   - 缓存失效策略

2. **优化数据库**
   - 添加索引
   - 优化查询
   - 分页优化

3. **异步优化**
   - 并发控制
   - 失败重试
   - 死信处理

### 优先级4：前后端联调

1. **API对接**
   - 修改前端API地址
   - 测试所有接口
   - 处理数据格式差异

2. **错误处理**
   - 统一错误码
   - 友好错误提示

3. **性能测试**
   - 压力测试
   - 并发测试
   - 性能调优

---

## 📚 参考文档

### 技术文档

- [Spring Boot 3.2 Documentation](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [MyBatis-Plus Documentation](https://baomidou.com/)
- [Neo4j Spring Data](https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

### 项目文档

- 开发规划：`.claude/plan/backend-development.md`
- PostgreSQL脚本：`src/main/resources/sql/init.sql`
- Neo4j脚本：`src/main/resources/sql/neo4j-init.cypher`

---

## 👥 开发团队

- **架构设计**: Claude Code + Codex MCP
- **代码实现**: Codex MCP (gpt-5-codex)
- **项目管理**: Claude Code

---

## 📄 许可证

本项目采用 MIT 许可证。

---

**最后更新**: 2025-10-03
**版本**: v1.0
**状态**: ✅ 核心功能已完成，待集成真实AI服务
