-- ============================================================================
-- AI知识库管理系统 - PostgreSQL数据库初始化脚本
-- ============================================================================
-- 版本: v1.0
-- 创建日期: 2025-10-03
-- 说明:
--   1. 创建所有业务表
--   2. 创建索引
--   3. 插入初始数据
-- ============================================================================

-- 1. 创建数据库（如果不存在）
-- CREATE DATABASE ai_middle_platform;

-- 2. 连接数据库
-- \c ai_middle_platform;

-- 3. 安装pgvector扩展（用于向量检索）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- 用户和权限相关表
-- ============================================================================

-- 4. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
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

-- 添加列注释
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN sys_user.name IS '用户姓名';
COMMENT ON COLUMN sys_user.avatar IS '头像URL';
COMMENT ON COLUMN sys_user.role IS '角色：admin|user';
COMMENT ON COLUMN sys_user.enabled IS '是否启用';

CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_role ON sys_user(role);

-- ============================================================================
-- 知识库相关表
-- ============================================================================

-- 5. 知识库表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
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

-- 添加列注释
COMMENT ON TABLE kb_knowledge_base IS '知识库表';
COMMENT ON COLUMN kb_knowledge_base.kb_id IS '知识库唯一标识';
COMMENT ON COLUMN kb_knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN kb_knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN kb_knowledge_base.owner_id IS '所有者ID';
COMMENT ON COLUMN kb_knowledge_base.file_count IS '文件数量';
COMMENT ON COLUMN kb_knowledge_base.enabled IS '是否启用';

CREATE INDEX idx_kb_owner ON kb_knowledge_base(owner_id);
CREATE INDEX idx_kb_created ON kb_knowledge_base(created_at DESC);

-- 6. 知识库成员表
CREATE TABLE IF NOT EXISTS kb_member (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, user_id)
);

-- 添加列注释
COMMENT ON TABLE kb_member IS '知识库成员表';
COMMENT ON COLUMN kb_member.role IS '角色：owner|editor|viewer';

CREATE INDEX idx_member_kb ON kb_member(kb_id);
CREATE INDEX idx_member_user ON kb_member(user_id);

-- ============================================================================
-- 文件和内容处理相关表
-- ============================================================================

CREATE TABLE IF NOT EXISTS file_detail (
    id VARCHAR(32) PRIMARY KEY,
    url VARCHAR(512) NOT NULL,
    size BIGINT,
    filename VARCHAR(256),
    original_filename VARCHAR(256),
    base_path VARCHAR(256),
    path VARCHAR(256),
    ext VARCHAR(32),
    content_type VARCHAR(128),
    platform VARCHAR(32),
    th_url VARCHAR(512),
    th_filename VARCHAR(256),
    th_size BIGINT,
    th_content_type VARCHAR(128),
    object_id VARCHAR(32),
    object_type VARCHAR(32),
    metadata TEXT,
    user_metadata TEXT,
    th_metadata TEXT,
    th_user_metadata TEXT,
    attr TEXT,
    file_acl VARCHAR(32),
    th_file_acl VARCHAR(32),
    hash_info TEXT,
    upload_id VARCHAR(128),
    upload_status INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE file_detail IS '文件记录表';
COMMENT ON COLUMN file_detail.id IS '文件id';
COMMENT ON COLUMN file_detail.url IS '文件访问地址';
COMMENT ON COLUMN file_detail.metadata IS '文件元数据';
COMMENT ON COLUMN file_detail.user_metadata IS '文件用户元数据';
COMMENT ON COLUMN file_detail.attr IS '附加属性';
COMMENT ON COLUMN file_detail.hash_info IS '哈希信息';

CREATE INDEX idx_file_detail_object ON file_detail(object_type, object_id);
CREATE INDEX idx_file_detail_upload ON file_detail(upload_id);

-- 8. 文件分片信息表
CREATE TABLE IF NOT EXISTS file_part_detail (
    id VARCHAR(32) PRIMARY KEY,
    platform VARCHAR(32),
    upload_id VARCHAR(128),
    e_tag VARCHAR(255),
    part_number INTEGER,
    part_size BIGINT,
    hash_info TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE file_part_detail IS '文件分片信息表，仅在手动分片上传时使用';
COMMENT ON COLUMN file_part_detail.id IS '分片id';
COMMENT ON COLUMN file_part_detail.upload_id IS '上传ID，仅在手动分片上传时使用';

CREATE INDEX idx_file_part_upload ON file_part_detail(upload_id);

-- 文档内容表 (每页一条记录)
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL UNIQUE,
    file_id VARCHAR(32) NOT NULL REFERENCES file_detail(id) ON DELETE CASCADE,
    page_index INTEGER NOT NULL,
    content TEXT,
    image_url VARCHAR(500),
    ocr_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    tokens_used INTEGER DEFAULT 0,
    ocr_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, page_index)
);

CREATE INDEX idx_kb_document_file_id ON kb_document(file_id);
CREATE INDEX idx_kb_document_status ON kb_document(ocr_status);

-- 触发器：自动更新 updated_at
CREATE OR REPLACE FUNCTION update_kb_document_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_kb_document_updated_at
    BEFORE UPDATE ON kb_document
    FOR EACH ROW
    EXECUTE FUNCTION update_kb_document_updated_at();

-- 8. 问答对表
CREATE TABLE IF NOT EXISTS kb_qa_pair (
    id BIGSERIAL PRIMARY KEY,
    qa_id VARCHAR(50) NOT NULL UNIQUE,
    file_id VARCHAR(32) NOT NULL REFERENCES file_detail(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_text TEXT NOT NULL,
    confidence_score DECIMAL(5,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加列注释
COMMENT ON TABLE kb_qa_pair IS '问答对表';
COMMENT ON COLUMN kb_qa_pair.qa_id IS '问答对唯一标识';
COMMENT ON COLUMN kb_qa_pair.question IS '问题';
COMMENT ON COLUMN kb_qa_pair.answer IS '答案';
COMMENT ON COLUMN kb_qa_pair.source_text IS '原文片段';
COMMENT ON COLUMN kb_qa_pair.confidence_score IS '置信度（0-1）';

CREATE INDEX idx_qa_file ON kb_qa_pair(file_id);
-- GIN索引用于全文搜索
CREATE INDEX idx_qa_question ON kb_qa_pair USING gin(to_tsvector('simple', question));

-- 9. 处理任务表
CREATE TABLE IF NOT EXISTS kb_process_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    file_id VARCHAR(32) NOT NULL REFERENCES file_detail(id) ON DELETE CASCADE,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加列注释
COMMENT ON TABLE kb_process_task IS '处理任务表';
COMMENT ON COLUMN kb_process_task.task_id IS '任务唯一标识';
COMMENT ON COLUMN kb_process_task.task_type IS '任务类型：OCR|VECTORIZATION|QA_GENERATION|KNOWLEDGE_GRAPH';
COMMENT ON COLUMN kb_process_task.status IS '状态：pending|processing|completed|failed';
COMMENT ON COLUMN kb_process_task.progress IS '进度（0-100）';
COMMENT ON COLUMN kb_process_task.error_message IS '错误信息';
COMMENT ON COLUMN kb_process_task.started_at IS '开始时间';
COMMENT ON COLUMN kb_process_task.completed_at IS '完成时间';

CREATE INDEX idx_task_file ON kb_process_task(file_id);
CREATE INDEX idx_task_status ON kb_process_task(status);
CREATE INDEX idx_task_type ON kb_process_task(task_type);

-- ============================================================================
-- 模型配置相关表
-- ============================================================================

-- 10. 业务配置表
CREATE TABLE IF NOT EXISTS model_business (
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

-- 添加列注释
COMMENT ON TABLE model_business IS '业务配置表';
COMMENT ON COLUMN model_business.business_id IS '业务唯一标识';
COMMENT ON COLUMN model_business.name IS '业务名称';
COMMENT ON COLUMN model_business.code IS '业务代码';
COMMENT ON COLUMN model_business.description IS '业务描述';
COMMENT ON COLUMN model_business.enabled IS '是否启用';

CREATE INDEX idx_business_code ON model_business(code);
CREATE INDEX idx_business_enabled ON model_business(enabled);

-- 11. 端点配置表
CREATE TABLE IF NOT EXISTS model_endpoint (
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

-- 添加列注释
COMMENT ON TABLE model_endpoint IS '端点配置表';
COMMENT ON COLUMN model_endpoint.endpoint_id IS '端点唯一标识';
COMMENT ON COLUMN model_endpoint.name IS '端点名称';
COMMENT ON COLUMN model_endpoint.base_url IS 'API基础URL';
COMMENT ON COLUMN model_endpoint.provider IS '提供商：OpenAI|Claude|Gemini|Azure|Custom';
COMMENT ON COLUMN model_endpoint.description IS '端点描述';
COMMENT ON COLUMN model_endpoint.enabled IS '是否启用';

CREATE INDEX idx_endpoint_provider ON model_endpoint(provider);
CREATE INDEX idx_endpoint_enabled ON model_endpoint(enabled);

-- 12. 模型信息表
CREATE TABLE IF NOT EXISTS model_info (
    id BIGSERIAL PRIMARY KEY,
    model_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加列注释
COMMENT ON TABLE model_info IS '模型信息表';
COMMENT ON COLUMN model_info.model_id IS '模型唯一标识';
COMMENT ON COLUMN model_info.name IS '模型名称';
COMMENT ON COLUMN model_info.provider IS '提供商';
COMMENT ON COLUMN model_info.description IS '模型描述';

CREATE INDEX idx_model_endpoint ON model_info(endpoint_id);

-- 13. 端点业务关联表
CREATE TABLE IF NOT EXISTS model_endpoint_business (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES model_business(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(endpoint_id, business_id)
);

COMMENT ON TABLE model_endpoint_business IS '端点业务关联表';

CREATE INDEX idx_eb_endpoint ON model_endpoint_business(endpoint_id);
CREATE INDEX idx_eb_business ON model_endpoint_business(business_id);

-- 14. API Key配置表
CREATE TABLE IF NOT EXISTS model_api_key (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(50) NOT NULL UNIQUE,
    endpoint_id BIGINT NOT NULL REFERENCES model_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    display_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 限流配置
    rate_limit_per_minute INT,
    rate_limit_per_day INT,

    -- 使用统计
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

-- 添加列注释
COMMENT ON TABLE model_api_key IS 'API Key配置表';
COMMENT ON COLUMN model_api_key.key_id IS 'Key唯一标识';
COMMENT ON COLUMN model_api_key.name IS 'Key名称';
COMMENT ON COLUMN model_api_key.api_key IS 'API Key（加密存储）';
COMMENT ON COLUMN model_api_key.display_key IS '显示用Key（脱敏）';
COMMENT ON COLUMN model_api_key.enabled IS '是否启用';
COMMENT ON COLUMN model_api_key.rate_limit_per_minute IS '每分钟请求限制';
COMMENT ON COLUMN model_api_key.rate_limit_per_day IS '每日请求限制';
COMMENT ON COLUMN model_api_key.total_requests IS '总请求数';
COMMENT ON COLUMN model_api_key.success_requests IS '成功请求数';
COMMENT ON COLUMN model_api_key.failed_requests IS '失败请求数';
COMMENT ON COLUMN model_api_key.last_error IS '最后错误信息';
COMMENT ON COLUMN model_api_key.expires_at IS '过期时间';
COMMENT ON COLUMN model_api_key.last_used_at IS '最后使用时间';

CREATE INDEX idx_apikey_endpoint ON model_api_key(endpoint_id);
CREATE INDEX idx_apikey_enabled ON model_api_key(enabled);
CREATE INDEX idx_apikey_expires ON model_api_key(expires_at);

-- ============================================================================
-- 插入初始数据
-- ============================================================================

-- 16. 插入系统管理员用户
-- 密码: admin123 (BCrypt加密后的值)
INSERT INTO sys_user (username, email, password, name, role)
VALUES ('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin')
ON CONFLICT (username) DO NOTHING;

-- 17. 插入测试用户
INSERT INTO sys_user (username, email, password, name, role)
VALUES ('testuser', 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 'user')
ON CONFLICT (username) DO NOTHING;

-- Add user: zang / zangzang (BCrypt strength 10)
INSERT INTO sys_user (username, email, password, name, role)
VALUES ('zang', 'zang@example.com', '$2a$10$1dpFZEqIALm1bkqZsQ5cROGvKL7mrhtDppjC5nPUjeSH8by06yW0G', 'Zang', 'admin')
ON CONFLICT (username) DO NOTHING;

-- 18. 插入默认业务配置
INSERT INTO model_business (business_id, name, code, description, enabled, created_by)
VALUES
    ('biz-1', 'OCR识别', 'ocr', 'OCR文字识别业务', TRUE, 1),
    ('biz-2', '文本向量化', 'vectorization', '文本向量化业务', TRUE, 1),
    ('biz-3', '知识图谱生成', 'kg', '知识图谱生成业务', TRUE, 1),
    ('biz-4', '问答对生成', 'qa', '问答对生成业务', TRUE, 1)
ON CONFLICT (business_id) DO NOTHING;

-- ============================================================================
-- 创建更新时间自动更新触发器
-- ============================================================================

-- 19. 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 20. 为需要的表添加触发器
CREATE TRIGGER update_sys_user_updated_at BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_kb_knowledge_base_updated_at BEFORE UPDATE ON kb_knowledge_base
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_business_updated_at BEFORE UPDATE ON model_business
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_endpoint_updated_at BEFORE UPDATE ON model_endpoint
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_api_key_updated_at BEFORE UPDATE ON model_api_key
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 验证
-- ============================================================================

-- 21. 查看所有表
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- 22. 查看所有索引
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- ============================================================================
-- Prompt 模板管理表
-- ============================================================================

-- 创建 Prompt 表
CREATE TABLE IF NOT EXISTS kb_prompt (
    id BIGSERIAL PRIMARY KEY,
    prompt_id VARCHAR(50) NOT NULL UNIQUE,
    business_code VARCHAR(50) NOT NULL UNIQUE,
    prompt_name VARCHAR(100) NOT NULL,
    prompt_content TEXT NOT NULL,
    description VARCHAR(500),
    variables TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加列注释
COMMENT ON TABLE kb_prompt IS 'AI Prompt 模板管理表';
COMMENT ON COLUMN kb_prompt.prompt_id IS 'Prompt UUID';
COMMENT ON COLUMN kb_prompt.business_code IS '业务代码：ocr|qa|kg|tag';
COMMENT ON COLUMN kb_prompt.prompt_name IS 'Prompt名称';
COMMENT ON COLUMN kb_prompt.prompt_content IS 'Prompt内容模板';
COMMENT ON COLUMN kb_prompt.description IS 'Prompt描述';
COMMENT ON COLUMN kb_prompt.variables IS '变量列表(JSON格式)';
COMMENT ON COLUMN kb_prompt.is_active IS '是否激活';
COMMENT ON COLUMN kb_prompt.version IS '版本号';

-- 创建索引
CREATE INDEX idx_prompt_business_code ON kb_prompt(business_code);
CREATE INDEX idx_prompt_is_active ON kb_prompt(is_active);

-- 插入初始 Prompt 数据

-- 1. OCR 识别 Prompt
INSERT INTO kb_prompt (prompt_id, business_code, prompt_name, prompt_content, description, variables, is_active, version)
VALUES (
    'ocr-default-001',
    'ocr',
    'OCR文字识别（默认）',
    '你是一个专业的OCR识别助手。请识别图片中的所有文字内容，包括：
1. 普通文字：保持原有格式和排版
2. 数学公式：使用正确的LaTeX格式表示，严格按照以下规则：
   - 行间公式用 $$...$$ 包围（注意是双美元符号）
   - 行内公式用 $...$ 包围（单美元符号）
   - 分段函数使用cases环境：$$\begin{cases} ... \end{cases}$$
   - cases环境中每行末尾必须用 \\\\ 分隔
   - 复杂公式示例：$$\begin{cases} x = a \cos t \\\\ y = b \sin t \end{cases}, \quad 0 \le t \le 2\pi$$
   - 矩阵使用matrix环境：$$\begin{matrix} ... \end{matrix}$$
   - 积分、求和等用标准LaTeX符号：\int, \sum, \prod
   - 分数用\frac{分子}{分母}格式
   - 上下标用^{}和_{}，多字符必须用大括号包围
3. 表格：使用Markdown表格格式
4. 代码：使用代码块格式
5. 图表描述：简要描述图表内容和数据

数学公式格式化重要提醒：
- 确保所有LaTeX环境（如cases, matrix, align等）都正确闭合
- 特殊字符如\, {, }必须正确转义
- 公式中的文字说明用\text{}包围
- 条件范围等附加信息放在公式外部，用逗号和\quad分隔

要求：
- 确保识别结果的准确性和完整性
- 保持原文的结构和格式
- 输出为Markdown格式
- 如果有多种语言，请都识别出来
- 数学公式必须能在支持MathJax的Markdown环境中正确渲染',
    'OCR文字识别的默认Prompt，支持普通文字、数学公式、表格、代码识别',
    '[]',
    true,
    1
);

-- 2. 问答对生成 Prompt
INSERT INTO kb_prompt (prompt_id, business_code, prompt_name, prompt_content, description, variables, is_active, version)
VALUES (
    'qa-default-001',
    'qa',
    '问答对生成（默认）',
    '根据以下文本片段生成{questionCount}个问答对。

要求：
1. 问题要针对文本片段中的具体信息
2. 答案要准确且简洁，直接从文本内容中提取
3. 每个问答对要独立且有价值
4. 返回JSON数组格式：[{"question": "...", "answer": "..."}]
5. 不要添加额外的说明文字，只返回JSON数组

文本片段：
{content}',
    '根据文本片段生成问答对，支持动态问题数量',
    '["questionCount", "content"]',
    true,
    1
);

-- 3. 知识图谱构建 Prompt
INSERT INTO kb_prompt (prompt_id, business_code, prompt_name, prompt_content, description, variables, is_active, version)
VALUES (
    'kg-default-001',
    'kg',
    '知识图谱构建（默认）',
    '你是一名资深的企业知识图谱构建专家，负责从多源文档中抽取高价值知识。

文档信息：
- 标题：{documentName}
- 文件类型：{fileType}
- 总页数：{totalPages}
- 当前批次：{chunkIndex}/{totalChunks}（页面：{pageSummary}）

任务要求：
1. 自动判断文档主题，并结合页面内容提取关键实体（组织、产品、技术、指标、事件、法规、人物等）。
2. 为每个实体提供专业、精炼的说明，明确其在文档语境中的角色。
3. 根据文本语义识别实体之间的关系，关系类型务必专业、可读，例如 "隶属于"、"依赖"、"调用"、"组成"、"授权"、"影响" 等。
4. 尽量复用既有的实体名称，如需使用别名请同样输出，后续将进行跨批次对齐。
5. 如果某类信息在当前批次不存在，请返回空数组，不要编造。
6. 为每个实体生成一个稳定且唯一的ID（推荐：英文小写短横线风格，例如 "product-api"），并在后续批次中保持一致；如果遇到已存在的实体，请复用同一个ID。

输出格式（仅返回 JSON，不要添加额外说明）：
{
  "entities": [
    {
      "id": "unique-entity-id",
      "name": "实体名称",
      "type": "实体类型（英文或驼峰，保持一致性）",
      "description": "50字以内的专业描述",
      "aliases": ["可选别名1", "可选别名2"],
      "sourcePages": [页面编号]
    }
  ],
  "relationships": [
    {
      "fromId": "源实体ID",
      "toId": "目标实体ID",
      "type": "关系类型（英文大写或驼峰）",
      "description": "关系说明",
      "sourcePages": [页面编号]
    }
  ]
}

文档内容：
{content}',
    '从文档中抽取实体和关系构建知识图谱',
    '["documentName", "fileType", "totalPages", "chunkIndex", "totalChunks", "pageSummary", "content"]',
    true,
    1
);

-- 4. 文件标签生成 Prompt
INSERT INTO kb_prompt (prompt_id, business_code, prompt_name, prompt_content, description, variables, is_active, version)
VALUES (
    'tag-default-001',
    'tag',
    '文件标签生成（默认）',
    '根据以下文件信息生成{tagCount}个左右的标签。

文件名：{fileName}
内容概览：
{content}

要求：
1. 标签要准确反映文件的主题、领域、技术栈或用途
2. 标签应该是常用的、简洁的词汇（2-6个字）
3. 返回5个左右最相关的标签
4. 标签可以是：技术类（如"机器学习"、"Python"）、领域类（如"金融"、"医疗"）、用途类（如"教程"、"API文档"）
5. 返回JSON数组格式：["标签1", "标签2", "标签3", "标签4", "标签5"]
6. 不要添加额外的说明文字，只返回JSON数组',
    '根据文件名和内容预览生成文件标签',
    '["tagCount", "fileName", "content"]',
    true,
    1
);

-- ============================================================================
-- 脚本结束
-- ============================================================================
