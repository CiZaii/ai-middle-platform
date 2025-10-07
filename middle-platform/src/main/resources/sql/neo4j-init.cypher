// ============================================================================
// AI知识库管理系统 - Neo4j图数据库初始化脚本
// ============================================================================
// 版本: v1.0
// 创建日期: 2025-10-03
// 说明:
//   1. 创建节点约束
//   2. 创建索引
//   3. 创建全文搜索索引
// ============================================================================

// ============================================================================
// 1. 创建唯一约束
// ============================================================================

// 文档节点ID唯一约束
CREATE CONSTRAINT doc_id IF NOT EXISTS
FOR (d:Document) REQUIRE d.id IS UNIQUE;

// 概念节点ID唯一约束
CREATE CONSTRAINT concept_id IF NOT EXISTS
FOR (c:Concept) REQUIRE c.id IS UNIQUE;

// 实体节点ID唯一约束
CREATE CONSTRAINT entity_id IF NOT EXISTS
FOR (e:Entity) REQUIRE e.id IS UNIQUE;

// 章节节点ID唯一约束
CREATE CONSTRAINT section_id IF NOT EXISTS
FOR (s:Section) REQUIRE s.id IS UNIQUE;

// ============================================================================
// 2. 创建属性索引
// ============================================================================

// 文档创建时间索引（用于时间排序）
CREATE INDEX doc_created IF NOT EXISTS
FOR (d:Document) ON (d.createdAt);

// 文档名称索引
CREATE INDEX doc_name IF NOT EXISTS
FOR (d:Document) ON (d.name);

// 实体类型索引（用于按类型过滤）
CREATE INDEX entity_type IF NOT EXISTS
FOR (e:Entity) ON (e.type);

// 实体名称索引
CREATE INDEX entity_name IF NOT EXISTS
FOR (e:Entity) ON (e.name);

// 概念类型索引
CREATE INDEX concept_type IF NOT EXISTS
FOR (c:Concept) ON (c.type);

// 概念名称索引
CREATE INDEX concept_name IF NOT EXISTS
FOR (c:Concept) ON (c.name);

// 章节标题索引
CREATE INDEX section_title IF NOT EXISTS
FOR (s:Section) ON (s.title);

// 章节层级索引
CREATE INDEX section_level IF NOT EXISTS
FOR (s:Section) ON (s.level);

// ============================================================================
// 3. 创建全文搜索索引
// ============================================================================

// 概念全文搜索索引（搜索名称和描述）
CREATE FULLTEXT INDEX concept_search IF NOT EXISTS
FOR (c:Concept) ON EACH [c.name, c.description];

// 实体全文搜索索引（搜索名称）
CREATE FULLTEXT INDEX entity_search IF NOT EXISTS
FOR (e:Entity) ON EACH [e.name];

// 章节全文搜索索引（搜索标题和内容）
CREATE FULLTEXT INDEX section_search IF NOT EXISTS
FOR (s:Section) ON EACH [s.title, s.content];

// 文档全文搜索索引
CREATE FULLTEXT INDEX document_search IF NOT EXISTS
FOR (d:Document) ON EACH [d.name];

// ============================================================================
// 4. 验证约束和索引
// ============================================================================

// 显示所有约束
SHOW CONSTRAINTS;

// 显示所有索引
SHOW INDEXES;

// ============================================================================
// 5. 示例：创建知识图谱数据
// ============================================================================

// 以下是创建知识图谱的示例查询，实际使用时由后端代码调用

// 创建文档节点
// MERGE (d:Document {
//     id: 'file-1',
//     name: 'API接口文档.pdf',
//     type: 'pdf',
//     createdAt: datetime()
// })

// 创建实体节点
// MERGE (e:Entity {
//     id: 'entity-1',
//     name: '用户认证',
//     type: 'module',
//     attributes: {description: '用户认证模块'}
// })

// 创建关系
// MATCH (d:Document {id: 'file-1'})
// MATCH (e:Entity {id: 'entity-1'})
// MERGE (e)-[:BELONGS_TO]->(d)

// ============================================================================
// 6. 常用查询示例
// ============================================================================

// 6.1 查询文档的所有实体
// MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(e:Entity)
// RETURN e

// 6.2 查询实体之间的关系
// MATCH (e1:Entity)-[r:INTERACTS_WITH]->(e2:Entity)
// WHERE e1.id = $entityId
// RETURN e1, r, e2

// 6.3 全文搜索概念
// CALL db.index.fulltext.queryNodes('concept_search', $searchTerm)
// YIELD node, score
// RETURN node, score
// ORDER BY score DESC
// LIMIT 10

// 6.4 查询知识图谱（深度2）
// MATCH path = (d:Document {id: $fileId})<-[:BELONGS_TO*1..2]-(n)
// RETURN path

// 6.5 查询最相关的实体（通过关系强度）
// MATCH (e1:Entity {id: $entityId})-[r:RELATED_TO]->(e2:Entity)
// WHERE r.strength > 0.5
// RETURN e2, r.strength
// ORDER BY r.strength DESC

// ============================================================================
// 7. 性能优化建议
// ============================================================================

// 定期更新统计信息（每天执行一次）
// CALL db.stats.retrieve("GRAPH COUNTS")

// 清理未使用的节点和关系
// MATCH (n)
// WHERE NOT (n)--()
// DELETE n

// ============================================================================
// 8. 数据清理（谨慎使用）
// ============================================================================

// 删除所有数据（谨慎！）
// MATCH (n)
// DETACH DELETE n

// 删除特定文档的所有数据
// MATCH (d:Document {id: $fileId})
// OPTIONAL MATCH (d)<-[:BELONGS_TO]-(n)
// DETACH DELETE d, n

// ============================================================================
// 脚本结束
// ============================================================================
