# 知识图谱功能优化日志

## 最新修改日期
2024-10-07 (第六次优化)

## 最新修改（第六次优化 - 完善显示和修复Next.js问题）

### 12. ✅ 修复react-pdf在Next.js中的canvas错误
**问题**: `Module not found: Can't resolve 'canvas'`
**原因**: react-pdf在服务端渲染时尝试导入Node.js的canvas模块
**解决方案**: 配置Next.js webpack忽略canvas，并使用动态导入

**修改文件**:
- `next.config.js` - 添加webpack配置
- `file-viewer.tsx` - 使用动态导入

**配置内容**:
```javascript
// next.config.js
webpack: (config, { isServer }) => {
  if (isServer) {
    config.resolve.alias.canvas = false;
  }
  config.externals = [...(config.externals || []), { canvas: 'canvas' }];
  return config;
}

// file-viewer.tsx - 动态导入
const Document = dynamic(
  () => import('react-pdf').then((mod) => mod.Document),
  { ssr: false }
);
```

### 13. ✅ 节点颜色随机生成（同类型一致）
**需求**: 每次刷新节点颜色随机，但同一类型保持相同颜色
**解决方案**: 使用哈希函数根据type生成稳定的随机颜色

**修改文件**:
- `knowledge-graph-view.tsx`

**实现逻辑**:
```typescript
// 根据类型名称生成稳定的颜色哈希
const generateRandomColor = (seed: string) => {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = Math.abs(hash % 360);
  const fill = `hsl(${hue}, 70%, 65%)`;
  const stroke = `hsl(${hue}, 75%, 45%)`;
  return { fill, stroke };
};

// 缓存已生成的颜色
const typeColorCache: Record<string, { fill: string; stroke: string }> = {};
```

**效果**:
- ✅ 每个类型有唯一随机颜色
- ✅ 同一类型的所有节点颜色相同
- ✅ 刷新页面后同类型颜色保持一致（基于类型名哈希）
- ✅ 颜色明亮且有良好对比度

### 14. ✅ 移除文档节点显示
**需求**: 不显示Document类型的文档节点，只显示实体节点
**解决方案**: 前后端同时过滤文档节点

**修改文件**:
- `FileServiceImpl.java` - 后端不返回文档节点
- `knowledge-graph-view.tsx` - 前端过滤文档节点

**后端修改**:
```java
// 不添加文档节点
// nodes.add(documentNode); // 已移除

// 不添加实体->文档的"属于"关系
// edges.add(belongsToEdge); // 已移除
```

**前端修改**:
```typescript
// 过滤掉文档节点
const filteredNodes = graphData.nodes.filter(node => node.type !== 'Document');

// 过滤掉与文档相关的边
const filteredEdges = graphData.edges.filter(
  edge => !documentNodeIds.has(edge.source) && !documentNodeIds.has(edge.target)
);
```

**效果**:
- ✅ 知识图谱只显示实体节点
- ✅ 不显示文档中心节点
- ✅ 不显示"属于"关系
- ✅ 只显示实体之间的语义关系

---

## 第五次优化（节点颜色和PDF预览）

### 10. ✅ 根据节点type属性显示不同颜色
**问题**: 知识图谱节点颜色映射与后端返回的type不匹配
**需求**: 根据后端返回的type（Person、Organization、Document等）渲染不同颜色
**解决方案**: 更新颜色映射，添加中文标签

**修改文件**:
- `knowledge-graph-view.tsx`

**新增节点类型颜色**:
- 🟣 **Document** (文档) - 紫色渐变
- 🔴 **Person** (人物) - 粉红色渐变  
- 🔵 **Organization** (组织) - 蓝色渐变
- 🟢 **Product** (产品) - 绿色渐变
- 🟠 **Technology** (技术) - 橙粉色渐变
- 🌈 **Project** (项目) - 青色渐变
- 💜 **Skill** (技能) - 淡紫色渐变
- 🎀 **Concept** (概念) - 粉紫色渐变

**效果**:
- 每种类型的节点都有独特的颜色
- 图例显示中文标签，更易理解
- 自动根据type属性匹配颜色

### 11. ✅ 使用x-file-storage + react-pdf增强PDF预览
**问题**: react-pdf遇到CORS限制，无法直接访问MinIO文件
**原因**: react-pdf需要下载PDF文件，触发跨域限制
**解决方案**: 后端使用x-file-storage下载文件+token认证，前端react-pdf多页滚动模式

**新增文件**:
- `FileProxyController.java` - 后端文件代理控制器

**修改文件**:
- `SecurityConfig.java` - 代理接口需要token认证（方案2）
- `file-viewer.tsx` - 完整的PDF阅读器功能
- `next.config.js` - 配置rewrites转发代理请求

**技术方案**:
```java
// 后端：使用x-file-storage下载文件
@GetMapping("/api/proxy/file")
public ResponseEntity<byte[]> proxyFile(@RequestParam String url) {
    byte[] fileContent = fileStorageService.download(url).bytes();
    // 设置CORS头，返回文件
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Access-Control-Allow-Origin", "*")
        .body(fileContent);
}
```

```typescript
// 前端：使用代理URL
const proxyUrl = `/api/proxy/file?url=${encodeURIComponent(fileUrl)}`;

// react-pdf渲染
<Document file={proxyUrl}>
  <Page pageNumber={pageNumber} />
</Document>
```

```javascript
// Next.js rewrites配置
async rewrites() {
  return [{
    source: '/api/proxy/:path*',
    destination: 'http://localhost:8080/api/proxy/:path*',
  }];
}
```

**PDF阅读器功能（完整版）**:
- ✅ **多页滚动模式** - 显示所有页面，流畅滚动浏览
- ✅ **缩放控制** - 放大/缩小/实际大小/适应宽度（50%-300%）
- ✅ **页面导航** - 上一页/下一页按钮
- ✅ **页码跳转** - 输入页码直接跳转
- ✅ **自动页码** - 滚动时自动更新当前页码
- ✅ **文本选择** - 支持复制PDF中的文本
- ✅ **下载按钮** - 一键下载原始PDF
- ✅ **Token认证** - 通过httpHeaders传递token（方案2）
- ✅ **优雅UI** - 顶部工具栏，灰色背景，白色页面
- ✅ **响应式** - 根据缩放比例自动调整

**工作流程**:
```
浏览器 → /api/proxy/file?url=xxx
    ↓ (Next.js rewrites)
后端 → fileStorageService.download(url)
    ↓ (x-file-storage)
MinIO → 返回文件字节流
    ↓
后端 → 添加CORS头
    ↓
前端 → react-pdf渲染
```

---

## 第七次优化（状态独立 + 完整删除功能）

### 12. ✅ OCR重新处理不影响其他状态
**问题**: 重新OCR时会重置所有状态（向量化、问答对、知识图谱）
**需求**: 各个处理状态应该互相独立，重新OCR不应影响其他已完成的处理
**解决方案**: 修改`restartOcrProcessing`方法，只重置OCR状态

**修改文件**:
- `FileServiceImpl.java` - `restartOcrProcessing`方法

**修改前**:
```java
private void restartOcrProcessing(FileDetail file) {
    cleanupExistingDocumentImages(file.getId());
    FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
    attributes.setOcrStatus(ProcessingStatus.PENDING.getCode());
    attributes.setVectorizationStatus(ProcessingStatus.PENDING.getCode());      // ❌ 重置
    attributes.setQaPairsStatus(ProcessingStatus.PENDING.getCode());            // ❌ 重置
    attributes.setKnowledgeGraphStatus(ProcessingStatus.PENDING.getCode());     // ❌ 重置
    // ...
}
```

**修改后**:
```java
private void restartOcrProcessing(FileDetail file) {
    cleanupExistingDocumentImages(file.getId());
    FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
    attributes.setOcrStatus(ProcessingStatus.PENDING.getCode());  // ✅ 只重置OCR
    // attributes.setVectorizationStatus(...);      // ✅ 保持不变
    // attributes.setQaPairsStatus(...);            // ✅ 保持不变
    // attributes.setKnowledgeGraphStatus(...);     // ✅ 保持不变
    // ...
}
```

**效果**:
- ✅ 重新OCR不会清空已生成的知识图谱
- ✅ 重新OCR不会清空已生成的问答对
- ✅ 重新OCR不会清空向量化数据
- ✅ 各个处理流程完全独立

### 13. ✅ 实现完整的文件删除功能
**需求**: 删除文件时需要清理所有相关数据
**解决方案**: 完善delete方法，删除8个方面的数据

**修改文件**:
- `FileServiceImpl.java` - `delete`方法
- 添加`JdbcTemplate`依赖用于删除向量数据

**删除内容清单**:
1. ✅ **文档页面和OCR图片** - 删除MinIO中的页面图片
2. ✅ **文档记录** - 删除kb_document表记录
3. ✅ **Neo4j知识图谱数据**:
   - 删除Document节点及其关系
   - 删除只属于该文档的Entity节点
4. ✅ **问答对数据** - 删除kb_qa_pair表记录
5. ✅ **向量化数据** - 删除vector_store表中的向量
6. ✅ **MinIO原始文件** - 删除原始上传文件
7. ✅ **缩略图** - 删除文件缩略图
8. ✅ **文件记录和标签** - 删除file_detail表记录（包含标签元数据）

**代码实现**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(String id) {
    // 1. 删除文档页面和OCR图片
    cleanupExistingDocumentImages(id);
    documentMapper.delete(new LambdaQueryWrapper<KbDocument>()
            .eq(KbDocument::getFileId, id));
    
    // 2. 删除Neo4j知识图谱
    documentNodeRepository.deleteDocumentWithRelations(id);
    // 删除只属于该文档的实体
    neo4jClient.query(
        "MATCH (e:Entity)-[:BELONGS_TO]->(d:Document {fileId: $fileId}) " +
        "WHERE NOT EXISTS { " +
        "  MATCH (e)-[:BELONGS_TO]->(other:Document) WHERE other.fileId <> $fileId " +
        "} DETACH DELETE e")
        .bind(id).to("fileId").run();
    
    // 3. 删除问答对
    qaPairMapper.delete(new LambdaQueryWrapper<KbQaPair>()
            .eq(KbQaPair::getFileId, id));
    
    // 4. 删除向量数据
    jdbcTemplate.update(
        "DELETE FROM vector_store WHERE metadata->>'file_id' = ?", id);
    
    // 5-6. 删除MinIO文件
    fileStorageService.delete(file.getUrl());
    fileStorageService.delete(file.getThUrl());
    
    // 7-8. 删除文件记录和更新计数
    fileDetailMapper.deleteById(id);
    updateKnowledgeBaseFileCount(knowledgeBase, -1);
}
```

**特性**:
- ✅ 完整清理所有关联数据
- ✅ 防止脏数据残留
- ✅ 异常处理保证事务一致性
- ✅ 详细的删除日志记录
- ✅ 支持级联删除（知识图谱、向量、问答对等）

**日志输出示例**:
```
开始删除文件及其所有关联数据: fileId=xxx
删除文档页面和OCR图片: fileId=xxx
删除文档记录: fileId=xxx, count=5
删除Neo4j知识图谱数据: fileId=xxx
删除Neo4j实体节点完成: fileId=xxx
删除问答对数据: fileId=xxx
删除问答对: fileId=xxx, count=12
删除向量化数据: fileId=xxx
删除向量数据: fileId=xxx, count=15
删除MinIO原始文件: fileId=xxx, url=http://...
MinIO文件删除成功: fileId=xxx
缩略图删除成功: fileId=xxx
删除文件记录: fileId=xxx
文件删除完成，所有关联数据已清理: fileId=xxx
```

---

## 第四次优化（优化显示和修复预览 - 已废弃）

### 8. ✅ 关系label显示description而非type
**问题**: 知识图谱中边的label显示的是关系类型（如"任职于"），而不是详细描述
**需求**: 用户希望显示description属性的内容
**解决方案**: 修改查询逻辑，优先使用description作为边的label

**修改文件**:
- `FileServiceImpl.java` (line 675-684)

**修改内容**:
```java
// 之前：只使用relationType
String label = StringUtils.hasText(relationType) ? relationType : "关联";

// 现在：优先使用description，降级到relationType，最后兜底"关联"
String description = toStringValue(relation.get("description"));
String label = StringUtils.hasText(description) ? description : 
              (StringUtils.hasText(relationType) ? relationType : "关联");
```

### 9. ✅ 修复文件预览X-Frame-Options问题
**问题**: 文件无法在iframe中显示，控制台报错：
```
Refused to display 'http://211.159.174.70:8085/' in a frame because it set 'X-Frame-Options' to 'deny'
```

**原因**: MinIO服务器设置了`X-Frame-Options: deny`，阻止在iframe中嵌入

**解决方案**: 添加后端文件代理接口，移除X-Frame-Options头

**新增文件**:
- `FileProxyController.java` - 文件代理控制器

**修改文件**:
- `file-viewer.tsx` - 使用代理URL

**代理接口功能**:
- 路径：`GET /api/file-proxy?url={原始文件URL}`
- 从MinIO获取文件内容
- 移除`X-Frame-Options`头
- 添加CORS头支持跨域
- 保持原始Content-Type
- 允许iframe嵌入

**前端修改**:
```typescript
// 使用代理URL
const proxyUrl = `/api/file-proxy?url=${encodeURIComponent(fileUrl)}`;

// PDF预览
<iframe src={`${proxyUrl}#view=FitH`} />

// 图片预览
<img src={proxyUrl} />
```

---

## 第三次优化（修复查询问题）

### 7. ✅ 优化查询逻辑，使用关系而非属性 + 修复映射问题
**问题**: 前台查询接口只返回文档节点，没有实体和关系
**原因1**: 查询依赖 `documentId` 属性，但使用关系查询更可靠
**原因2**: Spring Data Neo4j 的自动映射无法正确处理 `@Node` 实体的投影查询
**解决方案**: 使用 neo4jClient 直接查询并手动映射，通过 `BELONGS_TO` 关系查询实体

**修改文件**:
- `DocumentNodeRepository.java` - 添加 EntityNode import，更新查询注释
- `FileServiceImpl.java` - 使用 neo4jClient 直接查询

**修改内容**:
```cypher
-- 之前：依赖 documentId 属性
MATCH (source:Entity)-[rel]->(target:Entity)
WHERE source.documentId = $documentId
  AND target.documentId = $documentId
  AND type(rel) <> 'BELONGS_TO'

-- 现在：使用 BELONGS_TO 关系（neo4jClient）
MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(e:Entity)
RETURN e.id AS id, e.name AS name, e.type AS type

-- 实体间关系查询
MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(source:Entity)
MATCH (d)<-[:BELONGS_TO]-(target:Entity)
MATCH (source)-[rel]->(target)
WHERE type(rel) <> 'BELONGS_TO'
RETURN source.id, target.id, type(rel), rel.description
```

**为什么使用 neo4jClient 而不是 Repository**:
- Spring Data Neo4j 的 `@Node` 实体在投影查询时无法正确映射属性
- 日志显示：`Cannot retrieve a value for property externalId` 等警告
- neo4jClient 提供了更灵活的手动映射控制
- 查询结果直接转为 Map，可以精确控制字段提取

**优势**:
- ✅ 完全基于图关系，不依赖属性
- ✅ 更符合图数据库的设计理念
- ✅ 避免了 Spring Data 映射问题
- ✅ 查询更可靠，性能更好

---

## 第二次优化（修复保存问题）

### 5. ✅ 修复Neo4j删除查询
**问题**: 之前的删除查询使用了 `DELETE r, e, d`，可能导致删除失败
**解决方案**: 改用 `DETACH DELETE` 和 `FOREACH` 来正确删除节点和关系

**修改文件**:
- `DocumentNodeRepository.java` (line 29-41)

**修改内容**:
```cypher
-- 之前: 可能导致删除失败
MATCH (d:Document {id: $fileId})
OPTIONAL MATCH (d)<-[:BELONGS_TO]-(e:Entity)
OPTIONAL MATCH (e)-[r]-()
DELETE r, e, d

-- 现在: 使用DETACH DELETE确保删除成功
MATCH (d:Document {id: $fileId})
OPTIONAL MATCH (d)<-[:BELONGS_TO]-(e:Entity)
WITH d, collect(e) as entities
FOREACH (entity IN entities | DETACH DELETE entity)
DETACH DELETE d
```

### 6. ✅ 添加详细日志
**目的**: 方便调试知识图谱保存过程
**修改文件**:
- `AIProcessServiceImpl.java` (line 1488-1650)

**添加的日志**:
- 开始持久化时的实体和关系数量
- 删除旧数据的成功/失败
- 保存文档节点
- 每个实体保存的成功/失败
- 每个BELONGS_TO关系创建的成功/失败
- 每个实体关系保存的成功/失败
- 最终统计：保存成功的实体和关系数量

---

## 历史修改内容（第一次优化）

### 1. ✅ 关系类型使用中文谓语
**问题**: 关系名称包含主语和宾语，太冗长
**解决方案**: 
- 修改知识图谱提示词，明确要求使用简洁的中文谓语
- 关系类型示例：任职于、开发、使用、属于、管理等

**修改文件**:
- `/update_kg_prompt.sql` - 更新数据库中的提示词

**执行更新**:
```bash
# 连接PostgreSQL数据库执行
psql -U your_username -d your_database -f /Users/zang/Documents/IDEA/AI-Middle-Platform/update_kg_prompt.sql
```

### 2. ✅ 保留中文关系类型
**问题**: 原代码会把所有关系类型转换为英文大写
**解决方案**: 修改 `sanitizeRelationshipType` 方法，保留中文字符

**修改文件**:
- `AIProcessServiceImpl.java` (line 1604-1611)

**修改内容**:
```java
// 之前: 转换为英文大写
String sanitized = type.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
return StringUtils.hasText(sanitized) ? sanitized : "RELATED_TO";

// 现在: 保留中文，只去除特殊字符
String sanitized = type.trim().replaceAll("[\\s\\p{Punct}]+", "_");
return StringUtils.hasText(sanitized) ? sanitized : "关联";
```

### 3. ✅ 恢复文档节点
**问题**: 查询文件详情时只返回实体节点，没有文档中心节点
**解决方案**: 在 `buildKnowledgeGraph` 方法中添加文档节点，并添加实体到文档的"属于"关系

**修改文件**:
- `FileServiceImpl.java` (line 581-686)

**返回结构**:
```
文档节点 (Document)
  ↑ [属于]
实体1 (Person)
  → [任职于] → 实体2 (Organization)
  → [开发] → 实体3 (Product)
```

### 4. ✅ 完善删除逻辑
**问题**: 重新生成图谱时可能没有完全删除旧数据
**解决方案**: 改进 `deleteDocumentWithRelations` 查询，确保删除文档、实体和所有关系

**修改文件**:
- `DocumentNodeRepository.java` (line 29-39)

**修改内容**:
```cypher
-- 之前: 只删除BELONGS_TO关系的节点
MATCH (d:Document {id: $fileId}) 
OPTIONAL MATCH (d)<-[:BELONGS_TO]-(n) 
DETACH DELETE d, n

-- 现在: 删除所有相关实体和它们的关系
MATCH (d:Document {id: $fileId})
OPTIONAL MATCH (d)<-[:BELONGS_TO]-(e:Entity)
OPTIONAL MATCH (e)-[r]-()
DELETE r, e, d
```

## 🔍 问题诊断

如果知识图谱仍然只返回文档节点，按以下步骤排查：

### 1. 检查后端日志
重新生成知识图谱后，查看日志中的关键信息：
```
开始持久化知识图谱: fileId=..., entities=32, relationships=16
删除旧知识图谱数据成功: fileId=...
保存文档节点成功: id=..., name=...
保存实体节点成功: id=..., name=..., type=...  (应该有多条)
创建BELONGS_TO关系成功: entity=... -> document=...  (应该有多条)
保存关系成功: ... -[任职于]-> ...  (应该有多条)
知识图谱持久化完成: fileId=..., 实体=32/32, 关系=16/16
```

### 2. 检查错误日志
如果有保存失败，会看到错误日志：
```
保存实体节点失败: id=..., name=...
创建BELONGS_TO关系失败: entity=... -> document=...
保存关系失败: ... -[...]-> ...
```

### 3. 直接查询Neo4j数据库
```cypher
// 查看文档节点
MATCH (d:Document {id: "your-file-id"}) RETURN d

// 查看实体节点
MATCH (d:Document {id: "your-file-id"})<-[:BELONGS_TO]-(e:Entity) RETURN e

// 查看所有关系
MATCH (d:Document {id: "your-file-id"})<-[:BELONGS_TO]-(e:Entity)
OPTIONAL MATCH (e)-[r]->(e2:Entity)
RETURN e, r, e2
```

## 测试步骤

### 1. 更新数据库提示词（如果还没执行）
```bash
cd /Users/zang/Documents/IDEA/AI-Middle-Platform
psql -U postgres -d ai_platform -f update_kg_prompt.sql
```

### 2. 重启后端服务
```bash
# 编译并重启Spring Boot应用
cd middle-platform
mvn clean package -DskipTests
# 然后重启应用
```

### 3. 查看现有文件的知识图谱（测试查询修复）
1. 直接访问文件详情页，查看知识图谱
2. 应该能看到：
   - 1个文档节点（中心）
   - N个实体节点
   - 实体→文档的"属于"关系
   - 实体→实体的语义关系（中文谓语）

### 4. 重新生成知识图谱（测试完整流程）
1. 点击"重新构建知识图谱"按钮
2. 查看后端日志，确认：
   ```
   开始持久化知识图谱: fileId=..., entities=32, relationships=16
   删除旧知识图谱数据成功
   保存文档节点成功
   保存实体节点成功 (多条)
   创建BELONGS_TO关系成功 (多条)
   保存关系成功 (多条)
   知识图谱持久化完成: fileId=..., 实体=32/32, 关系=16/16
   buildKnowledgeGraph: found 32 entities from Neo4j
   ```

### 5. 验证最终结果
- ✅ API返回包含文档节点 + 多个实体节点
- ✅ 关系名称为中文谓语（如"任职于"、"开发"、"使用"）
- ✅ 每个实体都有"属于"关系指向文档
- ✅ 实体之间有语义关系
- ✅ 重新生成时旧数据被完全删除

## 预期效果对比

### 修改前
```
问题1: 关系名称冗长
- "臧世龙任职于北京汇智典通科技有限公司" ❌

问题2: 没有文档节点
- 只有实体节点，缺少中心节点 ❌

问题3: 英文关系类型
- WORKS_AT, DEVELOPS, USES ❌
```

### 修改后
```
改进1: 简洁的中文谓语
- "任职于" ✅

改进2: 有文档中心节点
- 文档节点作为中心，所有实体连接到它 ✅

改进3: 中文关系类型
- 任职于、开发、使用、属于 ✅

改进4: 完全清理旧数据
- 重新生成时删除所有相关节点和关系 ✅
```

## 技术细节

### 关系方向说明
- **实体 → 文档**: 实体通过"属于"关系指向文档（箭头方向：实体→文档）
- **实体 → 实体**: 实体间的语义关系（如：人物→[任职于]→组织）

### 删除策略
1. 匹配文档节点
2. 找到所有属于该文档的实体（通过BELONGS_TO关系）
3. 删除这些实体的所有关系（包括实体之间的关系）
4. 删除实体节点
5. 删除文档节点

这确保了完整的数据清理，不会留下孤立的节点或关系。
