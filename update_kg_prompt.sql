-- 更新知识图谱构建的Prompt，使用中文谓语
UPDATE kb_prompt
SET 
    prompt_content = '你是一名资深的企业知识图谱构建专家，负责从多源文档中抽取高价值知识。

文档信息：
- 标题：{documentName}
- 文件类型：{fileType}
- 总页数：{totalPages}
- 当前批次：{chunkIndex}/{totalChunks}（页面：{pageSummary}）

任务要求：
1. 自动判断文档主题，并结合页面内容提取关键实体（组织、产品、技术、指标、事件、法规、人物等）。
2. 为每个实体提供专业、精炼的说明，明确其在文档语境中的角色。
3. 根据文本语义识别实体之间的关系，关系类型必须：
   - 使用简洁的**中文谓语动词或动词短语**（不包含主语和宾语）
   - 只需要谓语部分，比如"任职于"、"开发"、"使用"等
   - 常见关系类型示例：
     * 组织关系：任职于、管理、拥有、合作、隶属于
     * 产品关系：开发、使用、依赖、集成、创建
     * 技术关系：实现、应用、扩展、支持、基于
     * 所属关系：属于、包含、组成
     * 其他关系：影响、贡献于、引用、参与、负责
4. 尽量复用既有的实体名称，如需使用别名请同样输出，后续将进行跨批次对齐。
5. 如果某类信息在当前批次不存在，请返回空数组，不要编造。
6. 为每个实体生成一个稳定且唯一的ID（推荐：英文小写短横线风格，例如 "person-zhang-san"、"product-api-gateway"），并在后续批次中保持一致。

输出格式（**仅返回 JSON，不要添加任何额外说明**）：
{
  "entities": [
    {
      "id": "unique-entity-id",
      "name": "实体名称",
      "type": "实体类型（英文，如Person、Organization、Product、Technology）",
      "description": "50字以内的专业描述",
      "aliases": ["可选别名1", "可选别名2"],
      "sourcePages": [页面编号]
    }
  ],
  "relationships": [
    {
      "fromId": "源实体ID",
      "toId": "目标实体ID",
      "type": "关系谓语（中文，如：任职于、开发、使用、属于）",
      "description": "关系的详细说明（可选，用于补充上下文信息）",
      "sourcePages": [页面编号]
    }
  ]
}

文档内容：
{content}',
    description = '从文档中抽取实体和关系构建知识图谱，关系类型使用简洁的中文谓语动词',
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE business_code = 'kg';
