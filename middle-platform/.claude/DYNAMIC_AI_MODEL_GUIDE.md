# 动态AI模型配置系统 - 使用指南

> **版本**: v1.0
> **更新日期**: 2025-10-04
> **适用范围**: AI知识库管理系统后端

---

## 📋 目录

1. [系统概述](#系统概述)
2. [核心功能](#核心功能)
3. [架构设计](#架构设计)
4. [数据库配置](#数据库配置)
5. [使用示例](#使用示例)
6. [API参考](#api参考)
7. [故障排查](#故障排查)
8. [最佳实践](#最佳实践)

---

## 系统概述

### 什么是动态AI模型配置系统?

这是一个完全数据库驱动的AI模型管理系统,允许你**无需修改代码**即可:

- ✅ 切换不同的AI提供商(OpenAI, Claude, Gemini, SiliconFlow等)
- ✅ 配置多个API密钥并自动负载均衡
- ✅ 为不同业务场景配置不同的模型
- ✅ 实时监控API密钥使用情况和健康状态
- ✅ 自动故障转移和密钥失效检测

### 支持的业务场景

系统内置4个业务场景,每个场景可以独立配置不同的AI提供商和模型:

| 业务代码 | 业务名称 | 说明 | 推荐模型 |
|---------|---------|------|---------|
| `ocr` | OCR识别 | 图片/PDF文字识别 | GPT-4o, Claude-3.5 |
| `vectorization` | 向量化 | 文本转向量存储 | text-embedding-3-small, BGE-M3 |
| `qa` | 问答生成 | 自动生成问答对 | GPT-4o, Claude-3.5 |
| `kg` | 知识图谱 | 提取实体和关系 | GPT-4o, Claude-3.5 |

---

## 核心功能

### 1. ✨ 基于业务代码的动态配置

系统根据**业务代码**自动查找对应的:
- 端点(Endpoint) - API服务地址
- API密钥(API Key) - 认证凭证
- 模型(Model) - 具体使用的模型名称

```java
// 代码中只需传入业务代码
String[] config = modelConfigService.getModelConfig("ocr");
// 返回: [baseUrl, apiKey, provider, modelName]
```

### 2. 🔄 智能负载均衡

**多API密钥自动轮换**:
- 优先使用**请求次数最少**的密钥
- 同等次数下,选择**最近未使用**的密钥
- 自动跳过**已禁用**和**已过期**的密钥

**负载均衡算法**:
```
排序规则:
1. totalRequests (总请求数) ↑
2. lastUsedAt (最后使用时间) ↑
→ 选择第一个可用密钥
```

### 3. 🛡️ 自动故障转移

**健康检测机制**:
- ✅ 检测`unauthorized`错误 → 自动禁用密钥
- ✅ 失败次数 >= 5 且 > 成功次数 → 自动禁用密钥
- ✅ 过期时间检查 → 自动跳过过期密钥
- ✅ 支持排除列表 → 重试时避免使用失败的密钥

**故障转移流程**:
```
请求业务配置 → 获取端点列表 → 遍历端点
  ↓
选择可用API密钥 → 排除已失败的密钥
  ↓
执行AI调用 → 记录使用统计
  ↓
失败? → 标记密钥 → 重试下一个密钥
成功? → 更新成功计数 → 返回结果
```

### 4. 📊 实时使用统计

每次API调用自动记录:
- `totalRequests` - 总请求数
- `successRequests` - 成功次数
- `failedRequests` - 失败次数
- `lastUsedAt` - 最后使用时间
- `lastError` - 最后错误信息

---

## 架构设计

### 数据库关系图

```
model_business (业务配置)
    ↓
model_endpoint_business (关联表)
    ↓
model_endpoint (端点配置) ←→ model_api_key (API密钥)
    ↓
model_info (模型信息)
```

### 核心组件

#### 1. ModelConfigService - 配置查询服务

**接口方法**:

```java
// 获取简化配置 (返回数组)
String[] getModelConfig(String businessCode);

// 获取完整运行时配置 (返回对象)
ModelRuntimeConfig getRuntimeConfig(String businessCode);

// 获取完整配置并排除指定密钥 (用于故障转移)
ModelRuntimeConfig getRuntimeConfig(String businessCode, Set<String> excludedKeyIds);

// 获取可用API密钥 (支持负载均衡)
ModelApiKey getAvailableApiKey(Long endpointId);
ModelApiKey getAvailableApiKey(Long endpointId, Set<String> excludedKeyIds);

// 记录API密钥使用情况
void recordApiKeyUsage(String keyId, boolean success, String error);
```

#### 2. AIModelFactory - AI客户端工厂

**创建ChatClient**:
```java
@Autowired
private AIModelFactory aiModelFactory;

// 根据业务代码创建ChatClient
ChatClient chatClient = aiModelFactory.createChatClient("ocr");
String result = chatClient.prompt()
    .user("Extract text from this image")
    .call()
    .content();
```

**创建EmbeddingModel**:
```java
// 根据业务代码创建EmbeddingModel
EmbeddingModel embeddingModel = aiModelFactory.createEmbeddingModel("vectorization");
```

#### 3. DynamicEmbeddingModelHandler - 动态Embedding适配器

**特点**:
- 实现Spring AI `EmbeddingModel`接口
- 使用Forest HTTP客户端调用远程API
- 自动从`vectorization`业务加载配置
- 支持任何OpenAI兼容的Embedding API

---

## 数据库配置

### 步骤1: 创建业务配置

**默认已存在的业务**:
```sql
-- 查看已有业务
SELECT * FROM model_business;

-- 结果示例:
-- id | business_id | name | code | description | enabled
-- 1  | uuid-xxx    | OCR识别 | ocr | OCR文字识别服务 | true
-- 2  | uuid-xxx    | 向量化  | vectorization | 文本向量化服务 | true
-- 3  | uuid-xxx    | 问答生成 | qa | 问答对生成服务 | true
-- 4  | uuid-xxx    | 知识图谱 | kg | 知识图谱生成服务 | true
```

**如需新增业务**:
```sql
INSERT INTO model_business (business_id, name, code, description, enabled, created_by)
VALUES
  (gen_random_uuid(), '翻译服务', 'translation', 'AI翻译服务', true, 1);
```

### 步骤2: 创建端点配置

**OpenAI端点**:
```sql
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, description, enabled, created_by)
VALUES
  ('ep-openai-001',
   'OpenAI官方端点',
   'https://api.openai.com',
   'OpenAI',
   'OpenAI官方API服务',
   true,
   1);
```

**SiliconFlow端点**:
```sql
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, description, enabled, created_by)
VALUES
  ('ep-siliconflow-001',
   'SiliconFlow端点',
   'https://api.siliconflow.cn',
   'SiliconFlow',
   'SiliconFlow API服务',
   true,
   1);
```

**Azure OpenAI端点**:
```sql
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, description, enabled, created_by)
VALUES
  ('ep-azure-001',
   'Azure OpenAI端点',
   'https://your-resource.openai.azure.com',
   'Azure',
   'Azure OpenAI服务',
   true,
   1);
```

**自定义端点**:
```sql
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, description, enabled, created_by)
VALUES
  ('ep-custom-001',
   '自定义API端点',
   'https://your-custom-api.com',
   'Custom',
   '自建API服务',
   true,
   1);
```

### 步骤3: 创建API密钥

**单个密钥**:
```sql
INSERT INTO model_api_key (
  key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day,
  total_requests, success_requests, failed_requests, created_by
)
VALUES
  ('key-openai-001',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
   'OpenAI Primary Key',
   'sk-your-actual-openai-api-key-here',
   'sk-***here',
   true,
   60,      -- 每分钟60次
   10000,   -- 每天10000次
   0, 0, 0, 1);
```

**多个密钥(负载均衡)**:
```sql
-- 密钥1
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-openai-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
  'OpenAI Key 1', 'sk-key1-xxx', 'sk-***1', true, 60, 10000, 0, 0, 0, 1);

-- 密钥2
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-openai-002',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
  'OpenAI Key 2', 'sk-key2-xxx', 'sk-***2', true, 60, 10000, 0, 0, 0, 1);

-- 密钥3 (备用)
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-openai-003',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
  'OpenAI Key 3 (Backup)', 'sk-key3-xxx', 'sk-***3', true, 60, 10000, 0, 0, 0, 1);
```

**设置过期时间**:
```sql
UPDATE model_api_key
SET expires_at = '2025-12-31 23:59:59'
WHERE key_id = 'key-openai-001';
```

### 步骤4: 创建模型信息

**GPT-4o (Chat模型)**:
```sql
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES
  ('model-gpt4o',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
   'gpt-4o',
   'OpenAI',
   'GPT-4 Optimized - 最新优化版本');
```

**text-embedding-3-small (Embedding模型)**:
```sql
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES
  ('model-embed-small',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-001'),
   'text-embedding-3-small',
   'OpenAI',
   'OpenAI Embedding - 1536维');
```

**BAAI/bge-m3 (SiliconFlow Embedding)**:
```sql
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES
  ('model-bge-m3',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-siliconflow-001'),
   'BAAI/bge-m3',
   'SiliconFlow',
   'BGE-M3多语言Embedding - 1024维');
```

### 步骤5: 关联端点和业务

**OCR业务使用OpenAI**:
```sql
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT
  e.id,
  b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-001'
  AND b.code = 'ocr';
```

**向量化业务使用SiliconFlow**:
```sql
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT
  e.id,
  b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-siliconflow-001'
  AND b.code = 'vectorization';
```

**批量关联(所有业务使用同一端点)**:
```sql
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT
  e.id,
  b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-001'
  AND b.code IN ('ocr', 'qa', 'kg');
```

---

## 使用示例

### 场景1: 新项目初始化

**完整SQL脚本**:

```sql
-- 1. 确认业务已存在
SELECT * FROM model_business WHERE code IN ('ocr', 'vectorization', 'qa', 'kg');

-- 2. 创建OpenAI端点
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, enabled, created_by)
VALUES ('ep-openai-main', 'OpenAI Main', 'https://api.openai.com', 'OpenAI', true, 1);

-- 3. 添加主API密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-main-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'Main API Key',
  'sk-your-openai-api-key',
  'sk-***key',
  true, 60, 10000, 0, 0, 0, 1);

-- 4. 添加备用API密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-backup-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'Backup API Key',
  'sk-your-backup-key',
  'sk-***bak',
  true, 60, 10000, 0, 0, 0, 1);

-- 5. 创建模型信息 (Chat)
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES ('model-chat-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'gpt-4o', 'OpenAI', 'GPT-4 Optimized');

-- 6. 创建模型信息 (Embedding)
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES ('model-embed-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'text-embedding-3-small', 'OpenAI', 'Embedding 1536D');

-- 7. 关联所有业务到OpenAI端点
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-main'
  AND b.code IN ('ocr', 'vectorization', 'qa', 'kg');
```

### 场景2: 切换向量化服务提供商

**从OpenAI切换到SiliconFlow**:

```sql
-- 1. 创建SiliconFlow端点
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, enabled, created_by)
VALUES ('ep-silicon-001', 'SiliconFlow', 'https://api.siliconflow.cn', 'SiliconFlow', true, 1);

-- 2. 添加API密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-silicon-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-silicon-001'),
  'SiliconFlow Key',
  'sk-silicon-api-key',
  'sk-***con',
  true, 60, 10000, 0, 0, 0, 1);

-- 3. 添加BGE-M3模型
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES ('model-bge-m3-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-silicon-001'),
  'BAAI/bge-m3', 'SiliconFlow', 'BGE-M3 1024D');

-- 4. 切换vectorization业务到SiliconFlow
-- 先删除旧关联
DELETE FROM model_endpoint_business
WHERE business_id = (SELECT id FROM model_business WHERE code = 'vectorization');

-- 再添加新关联
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-silicon-001'
  AND b.code = 'vectorization';
```

**切换后立即生效,无需重启应用!**

### 场景3: 增加API密钥负载均衡

**为OCR服务添加多个密钥**:

```sql
-- 假设OCR使用的端点ID为 'ep-openai-main'

-- 添加第2个密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-ocr-002',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'OCR Key 2',
  'sk-second-key',
  'sk-***002',
  true, 60, 10000, 0, 0, 0, 1);

-- 添加第3个密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-ocr-003',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
  'OCR Key 3',
  'sk-third-key',
  'sk-***003',
  true, 60, 10000, 0, 0, 0, 1);
```

**系统会自动在这3个密钥之间负载均衡!**

### 场景4: 禁用和启用API密钥

**临时禁用密钥**:
```sql
UPDATE model_api_key
SET enabled = false
WHERE key_id = 'key-ocr-002';
```

**重新启用密钥**:
```sql
UPDATE model_api_key
SET enabled = true
WHERE key_id = 'key-ocr-002';
```

**查看密钥使用统计**:
```sql
SELECT
  key_id,
  name,
  total_requests,
  success_requests,
  failed_requests,
  ROUND(success_requests::numeric / NULLIF(total_requests, 0) * 100, 2) AS success_rate,
  last_used_at,
  last_error,
  enabled
FROM model_api_key
WHERE endpoint_id = (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main')
ORDER BY total_requests DESC;
```

### 场景5: 混合使用多个提供商

**OCR使用OpenAI,向量化使用SiliconFlow,QA使用Claude**:

```sql
-- 创建Claude端点
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, enabled, created_by)
VALUES ('ep-claude-001', 'Claude API', 'https://api.anthropic.com', 'Claude', true, 1);

-- 添加Claude密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by)
VALUES ('key-claude-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-claude-001'),
  'Claude Key',
  'sk-ant-xxx',
  'sk-***ude',
  true, 60, 10000, 0, 0, 0, 1);

-- 添加Claude模型
INSERT INTO model_info (model_id, endpoint_id, name, provider, description)
VALUES ('model-claude-001',
  (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-claude-001'),
  'claude-3-5-sonnet-20241022', 'Claude', 'Claude 3.5 Sonnet');

-- 配置业务关联
-- OCR → OpenAI
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-main' AND b.code = 'ocr';

-- Vectorization → SiliconFlow
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-silicon-001' AND b.code = 'vectorization';

-- QA → Claude
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-claude-001' AND b.code = 'qa';

-- KG → OpenAI
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-main' AND b.code = 'kg';
```

---

## API参考

### ModelConfigService

#### getModelConfig()

**方法签名**:
```java
String[] getModelConfig(String businessCode);
```

**参数**:
- `businessCode` - 业务代码 (`ocr`, `vectorization`, `qa`, `kg`)

**返回值**:
```java
String[4] = {
  [0] baseUrl,      // 如: "https://api.openai.com"
  [1] apiKey,       // 如: "sk-xxx"
  [2] provider,     // 如: "OpenAI"
  [3] modelName     // 如: "gpt-4o"
}
```

**使用示例**:
```java
@Autowired
private ModelConfigService modelConfigService;

public void example() {
    String[] config = modelConfigService.getModelConfig("ocr");
    String baseUrl = config[0];
    String apiKey = config[1];
    String provider = config[2];
    String modelName = config[3];

    System.out.println("OCR服务配置:");
    System.out.println("  Base URL: " + baseUrl);
    System.out.println("  Provider: " + provider);
    System.out.println("  Model: " + modelName);
}
```

#### getRuntimeConfig()

**方法签名**:
```java
ModelRuntimeConfig getRuntimeConfig(String businessCode);
ModelRuntimeConfig getRuntimeConfig(String businessCode, Set<String> excludedKeyIds);
```

**参数**:
- `businessCode` - 业务代码
- `excludedKeyIds` - 需要排除的API密钥ID集合(用于故障转移)

**返回值**:
```java
record ModelRuntimeConfig(
    ModelBusiness business,
    ModelEndpoint endpoint,
    ModelApiKey apiKey,
    ModelInfo modelInfo
) {
    String provider();           // 提供商名称
    String baseUrl();            // 基础URL
    String chatModelName();      // Chat模型名称
    String embeddingModelName(); // Embedding模型名称
}
```

**使用示例**:
```java
// 获取完整配置
ModelRuntimeConfig config = modelConfigService.getRuntimeConfig("vectorization");
String provider = config.provider();
String baseUrl = config.baseUrl();
String modelName = config.embeddingModelName();
String apiKey = config.apiKey().getApiKey();

// 故障转移 - 排除失败的密钥
Set<String> excludedKeys = Set.of("key-failed-001", "key-failed-002");
ModelRuntimeConfig retryConfig = modelConfigService.getRuntimeConfig("ocr", excludedKeys);
```

#### recordApiKeyUsage()

**方法签名**:
```java
void recordApiKeyUsage(String keyId, boolean success, String error);
```

**参数**:
- `keyId` - API密钥ID
- `success` - 是否成功
- `error` - 错误信息(成功时为null)

**使用示例**:
```java
try {
    // 执行AI调用
    String result = chatClient.call("...");

    // 记录成功
    modelConfigService.recordApiKeyUsage("key-openai-001", true, null);

} catch (Exception e) {
    // 记录失败
    modelConfigService.recordApiKeyUsage("key-openai-001", false, e.getMessage());
}
```

**自动健康检测**:
- 如果错误包含`unauthorized` → 自动禁用密钥
- 如果失败次数 >= 5 且 > 成功次数 → 自动禁用密钥

---

## 故障排查

### 问题1: "Business not available"

**错误信息**:
```
BusinessException: Business not available: ocr
```

**原因**:
- 业务不存在或已禁用

**解决方案**:
```sql
-- 检查业务是否存在
SELECT * FROM model_business WHERE code = 'ocr';

-- 启用业务
UPDATE model_business SET enabled = true WHERE code = 'ocr';
```

### 问题2: "No endpoint configured for business"

**错误信息**:
```
BusinessException: No endpoint configured for business: vectorization
```

**原因**:
- 业务没有关联任何端点

**解决方案**:
```sql
-- 检查关联
SELECT eb.*, e.name AS endpoint_name, b.name AS business_name
FROM model_endpoint_business eb
JOIN model_endpoint e ON eb.endpoint_id = e.id
JOIN model_business b ON eb.business_id = b.id
WHERE b.code = 'vectorization';

-- 创建关联
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id
FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-001' AND b.code = 'vectorization';
```

### 问题3: "No available API key"

**错误信息**:
```
BusinessException: No available API key for business: qa
```

**原因**:
- 端点没有可用的API密钥
- 所有密钥都已禁用或过期

**解决方案**:
```sql
-- 检查密钥状态
SELECT k.*, e.name AS endpoint_name
FROM model_api_key k
JOIN model_endpoint e ON k.endpoint_id = e.id
WHERE e.endpoint_id = 'ep-openai-001';

-- 启用密钥
UPDATE model_api_key SET enabled = true WHERE key_id = 'key-openai-001';

-- 清除过期时间
UPDATE model_api_key SET expires_at = NULL WHERE key_id = 'key-openai-001';
```

### 问题4: 密钥被自动禁用

**现象**:
- 密钥原本是启用的,运行一段时间后自动变为禁用

**原因**:
- 触发了健康检测机制
  - `unauthorized`错误
  - 失败次数过多(>= 5 且 > 成功次数)

**解决方案**:
```sql
-- 查看失败原因
SELECT key_id, name, failed_requests, success_requests, last_error, enabled
FROM model_api_key
WHERE enabled = false;

-- 修复问题后重新启用
UPDATE model_api_key
SET enabled = true,
    failed_requests = 0,
    last_error = NULL
WHERE key_id = 'key-openai-001';
```

### 问题5: Forest HTTP调用失败

**错误信息**:
```
Failed to create embedding for instruction: xxx
```

**排查步骤**:

1. **检查URL是否正确**:
```sql
SELECT endpoint_id, name, base_url, provider
FROM model_endpoint
WHERE endpoint_id = 'ep-silicon-001';
```

2. **检查API密钥是否有效**:
```bash
# 手动测试
curl -X POST https://api.siliconflow.cn/v1/embeddings \
  -H "Authorization: Bearer sk-your-key" \
  -H "Content-Type: application/json" \
  -d '{"input": "test", "model": "BAAI/bge-m3"}'
```

3. **查看应用日志**:
```bash
# 查找Forest相关日志
grep -i "forest" application.log
grep -i "embedding" application.log
```

---

## 最佳实践

### 1. 🔐 API密钥管理

**建议**:
- ✅ 为每个端点配置至少2个API密钥(主+备)
- ✅ 定期轮换密钥,设置过期时间
- ✅ 不同业务使用不同的密钥(隔离配额)
- ✅ 生产环境密钥与测试环境分离

**示例配置**:
```sql
-- 主密钥 (高配额)
INSERT INTO model_api_key (...)
VALUES ('key-prod-001', ..., rate_limit_per_day = 100000, ...);

-- 备用密钥 (中等配额)
INSERT INTO model_api_key (...)
VALUES ('key-prod-002', ..., rate_limit_per_day = 50000, ...);

-- 测试密钥 (低配额)
INSERT INTO model_api_key (...)
VALUES ('key-test-001', ..., rate_limit_per_day = 1000, ...);
```

### 2. 📊 监控和告警

**定期检查密钥健康状态**:
```sql
-- 查看高失败率密钥
SELECT
  key_id,
  name,
  total_requests,
  success_requests,
  failed_requests,
  ROUND(failed_requests::numeric / NULLIF(total_requests, 0) * 100, 2) AS fail_rate
FROM model_api_key
WHERE total_requests > 0
  AND failed_requests::numeric / NULLIF(total_requests, 0) > 0.1  -- 失败率 > 10%
ORDER BY fail_rate DESC;
```

**查看即将过期的密钥**:
```sql
SELECT key_id, name, expires_at
FROM model_api_key
WHERE expires_at IS NOT NULL
  AND expires_at < NOW() + INTERVAL '7 days'
  AND enabled = true;
```

### 3. 🚀 性能优化

**启用缓存** (可选):
```java
@Service
public class CachedModelConfigService implements ModelConfigService {

    @Cacheable(value = "modelConfig", key = "#businessCode")
    public String[] getModelConfig(String businessCode) {
        // 缓存配置,减少数据库查询
    }
}
```

### 4. 🔄 故障转移策略

**多端点冗余**:
```sql
-- 为同一业务配置多个端点
-- OCR业务 → 主端点: OpenAI
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-openai-001' AND b.code = 'ocr';

-- OCR业务 → 备用端点: Azure
INSERT INTO model_endpoint_business (endpoint_id, business_id)
SELECT e.id, b.id FROM model_endpoint e, model_business b
WHERE e.endpoint_id = 'ep-azure-001' AND b.code = 'ocr';
```

系统会自动按顺序尝试所有端点!

### 5. 📝 配置版本管理

**记录配置变更**:
```sql
-- 创建配置变更日志表(可选)
CREATE TABLE model_config_audit (
    id BIGSERIAL PRIMARY KEY,
    business_code VARCHAR(50),
    endpoint_id VARCHAR(100),
    action VARCHAR(20),  -- 'ADD', 'REMOVE', 'UPDATE'
    changed_by BIGINT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);

-- 记录变更
INSERT INTO model_config_audit (business_code, endpoint_id, action, changed_by, description)
VALUES ('vectorization', 'ep-silicon-001', 'ADD', 1, '切换向量化服务到SiliconFlow');
```

---

## 附录

### A. 完整示例数据

```sql
-- 创建完整的测试环境配置

-- 1. 业务配置
INSERT INTO model_business (business_id, name, code, description, enabled, created_by) VALUES
  (gen_random_uuid(), 'OCR识别', 'ocr', 'OCR文字识别服务', true, 1),
  (gen_random_uuid(), '向量化', 'vectorization', '文本向量化服务', true, 1),
  (gen_random_uuid(), '问答生成', 'qa', '问答对生成服务', true, 1),
  (gen_random_uuid(), '知识图谱', 'kg', '知识图谱生成服务', true, 1);

-- 2. 端点配置
INSERT INTO model_endpoint (endpoint_id, name, base_url, provider, enabled, created_by) VALUES
  ('ep-openai-main', 'OpenAI Main', 'https://api.openai.com', 'OpenAI', true, 1),
  ('ep-silicon-embed', 'SiliconFlow Embed', 'https://api.siliconflow.cn', 'SiliconFlow', true, 1);

-- 3. API密钥
INSERT INTO model_api_key (key_id, endpoint_id, name, api_key, display_key, enabled,
  rate_limit_per_minute, rate_limit_per_day, total_requests, success_requests, failed_requests, created_by) VALUES
  -- OpenAI密钥
  ('key-openai-001',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   'OpenAI Key 1', 'sk-openai-key-1', 'sk-***ai1', true, 60, 10000, 0, 0, 0, 1),
  ('key-openai-002',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   'OpenAI Key 2', 'sk-openai-key-2', 'sk-***ai2', true, 60, 10000, 0, 0, 0, 1),
  -- SiliconFlow密钥
  ('key-silicon-001',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-silicon-embed'),
   'SiliconFlow Key', 'sk-silicon-key', 'sk-***con', true, 60, 10000, 0, 0, 0, 1);

-- 4. 模型信息
INSERT INTO model_info (model_id, endpoint_id, name, provider, description) VALUES
  -- OpenAI模型
  ('model-gpt4o',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   'gpt-4o', 'OpenAI', 'GPT-4 Optimized'),
  ('model-embed-small',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   'text-embedding-3-small', 'OpenAI', 'OpenAI Embedding 1536D'),
  -- SiliconFlow模型
  ('model-bge-m3',
   (SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-silicon-embed'),
   'BAAI/bge-m3', 'SiliconFlow', 'BGE-M3 1024D');

-- 5. 业务端点关联
INSERT INTO model_endpoint_business (endpoint_id, business_id) VALUES
  -- OCR, QA, KG使用OpenAI
  ((SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   (SELECT id FROM model_business WHERE code = 'ocr')),
  ((SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   (SELECT id FROM model_business WHERE code = 'qa')),
  ((SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-openai-main'),
   (SELECT id FROM model_business WHERE code = 'kg')),
  -- Vectorization使用SiliconFlow
  ((SELECT id FROM model_endpoint WHERE endpoint_id = 'ep-silicon-embed'),
   (SELECT id FROM model_business WHERE code = 'vectorization'));
```

### B. 常用查询语句

```sql
-- 查看完整配置
SELECT
  b.code AS business,
  e.name AS endpoint,
  e.provider,
  k.name AS api_key_name,
  k.enabled AS key_enabled,
  m.name AS model_name
FROM model_business b
JOIN model_endpoint_business eb ON b.id = eb.business_id
JOIN model_endpoint e ON eb.endpoint_id = e.id
LEFT JOIN model_api_key k ON e.id = k.endpoint_id AND k.enabled = true
LEFT JOIN model_info m ON e.id = m.endpoint_id
ORDER BY b.code, k.total_requests;

-- 查看API密钥使用统计
SELECT
  e.name AS endpoint,
  k.name AS key_name,
  k.total_requests,
  k.success_requests,
  k.failed_requests,
  ROUND(k.success_requests::numeric / NULLIF(k.total_requests, 0) * 100, 2) AS success_rate,
  k.last_used_at,
  k.enabled
FROM model_api_key k
JOIN model_endpoint e ON k.endpoint_id = e.id
ORDER BY k.total_requests DESC;

-- 查看业务配置详情
SELECT
  b.code,
  b.name,
  e.provider,
  e.base_url,
  m.name AS model_name,
  COUNT(k.id) AS api_key_count
FROM model_business b
JOIN model_endpoint_business eb ON b.id = eb.business_id
JOIN model_endpoint e ON eb.endpoint_id = e.id
LEFT JOIN model_api_key k ON e.id = k.endpoint_id AND k.enabled = true
LEFT JOIN model_info m ON e.id = m.endpoint_id
GROUP BY b.code, b.name, e.provider, e.base_url, m.name
ORDER BY b.code;
```

---

## 总结

通过这套动态AI模型配置系统,你可以:

✅ **灵活配置** - 无需修改代码,数据库即可配置所有AI服务
✅ **负载均衡** - 多API密钥自动轮换,提升可用性
✅ **故障转移** - 自动检测失败,切换到备用密钥
✅ **多提供商** - 同时使用OpenAI、SiliconFlow、Claude等
✅ **实时监控** - 统计使用情况,健康状态一目了然

**需要帮助?**
- 查看日志: `tail -f logs/application.log | grep -i "model"`
- 数据库管理: `psql -h localhost -U postgres -d ai_middle_platform`
- 问题反馈: 联系开发团队

---

**文档版本**: v1.0
**最后更新**: 2025-10-04
**维护者**: AI Middle Platform Team
