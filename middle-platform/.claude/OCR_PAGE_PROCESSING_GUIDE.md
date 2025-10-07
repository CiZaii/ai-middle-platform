# OCR分页处理架构 - 技术文档

> **版本**: v2.0
> **更新日期**: 2025-10-04
> **重大变更**: 从整文件OCR升级到分页OCR处理

---

## 📋 目录

1. [架构概述](#架构概述)
2. [核心变更](#核心变更)
3. [数据库设计](#数据库设计)
4. [处理流程](#处理流程)
5. [代码实现](#代码实现)
6. [使用指南](#使用指南)
7. [故障排查](#故障排查)

---

## 架构概述

### 为什么需要分页处理?

**旧架构问题**:
- ❌ 整个文件发送给AI模型,超出token限制
- ❌ 大文件处理失败,无法重试单个页面
- ❌ OCR内容存储在`kb_file`表,数据臃肿
- ❌ 无法查看单页OCR结果

**新架构优势**:
- ✅ 按页拆分,每页独立OCR,不超token限制
- ✅ 单页失败可重试,不影响其他页
- ✅ OCR内容存储在`kb_document`表,每页一条记录
- ✅ 支持查看单页OCR结果和进度
- ✅ 自动格式转换: Word → PDF → 图片
- ✅ 所有页完成后自动触发向量化

---

## 核心变更

### 架构对比

#### 旧架构 (v1.0)
```
上传文件 → 发送整个文件URL → OCR识别 → 保存到kb_file.ocr_content
    ↓
向量化
```

**问题**:
- 大文件超过GPT-4o上下文限制(128K tokens)
- 失败无法重试单个部分
- 无法查看处理进度

#### 新架构 (v2.0)
```
上传文件 → 文件格式转换 → 拆分页面 → 上传页面图片
    ↓              ↓              ↓
  Word → PDF  |  PDF → JPG  |  Image → JPG
                   ↓
          创建kb_document记录(每页一条)
                   ↓
          发送OCR任务(每页一个消息)
                   ↓
          并发OCR处理(每页独立)
                   ↓
          所有页完成 → 聚合内容 → 触发向量化
```

**优势**:
- 每页图片发送,不超token限制
- 并发处理,速度更快
- 单页失败可重试
- 进度可视化

### 数据模型变更

#### 新增: kb_document表

**用途**: 存储每页的OCR内容和状态

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| document_id | VARCHAR(50) | 文档ID (唯一) |
| file_id | BIGINT | 关联的文件ID |
| page_index | INTEGER | 页码 (从1开始) |
| content | TEXT | OCR识别的文本内容 |
| image_url | VARCHAR(500) | 页面图片URL |
| ocr_status | VARCHAR(20) | pending/processing/completed/failed |
| tokens_used | INTEGER | 该页消耗的token数 |
| ocr_error | TEXT | OCR失败原因 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**索引**:
- `UNIQUE(file_id, page_index)` - 确保同一文件的页码唯一
- `idx_kb_document_file_id` - 快速查找文件的所有页面
- `idx_kb_document_status` - 快速查找待处理页面

#### 修改: kb_file表

**移除字段**: `ocr_content` (OCR内容现在存储在kb_document表)

**保留字段**: `ocr_status` (文件级别的总体状态)

---

## 数据库设计

### SQL迁移脚本

```sql
-- 1. 创建kb_document表
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(50) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL REFERENCES kb_file(id) ON DELETE CASCADE,
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

-- 2. 创建索引
CREATE INDEX idx_kb_document_file_id ON kb_document(file_id);
CREATE INDEX idx_kb_document_status ON kb_document(ocr_status);

-- 3. 创建触发器 (自动更新updated_at)
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

-- 4. (可选) 迁移现有数据
-- 如果kb_file中有ocr_content,可以迁移到kb_document
INSERT INTO kb_document (document_id, file_id, page_index, content, ocr_status)
SELECT
    gen_random_uuid()::text,
    id,
    1,  -- 旧数据视为单页
    ocr_content,
    CASE
        WHEN ocr_status = 'completed' THEN 'completed'
        ELSE 'pending'
    END
FROM kb_file
WHERE ocr_content IS NOT NULL AND ocr_content != '';

-- 5. 删除kb_file.ocr_content列
ALTER TABLE kb_file DROP COLUMN IF EXISTS ocr_content;
```

### 查询示例

```sql
-- 查看文件的所有页面及OCR状态
SELECT
    d.page_index,
    d.ocr_status,
    LENGTH(d.content) AS content_length,
    d.tokens_used,
    d.ocr_error
FROM kb_document d
WHERE d.file_id = (SELECT id FROM kb_file WHERE file_id = 'your-file-id')
ORDER BY d.page_index;

-- 查看文件OCR进度
SELECT
    f.file_id,
    f.name,
    COUNT(d.id) AS total_pages,
    COUNT(CASE WHEN d.ocr_status = 'completed' THEN 1 END) AS completed_pages,
    COUNT(CASE WHEN d.ocr_status = 'failed' THEN 1 END) AS failed_pages,
    ROUND(
        COUNT(CASE WHEN d.ocr_status = 'completed' THEN 1 END)::numeric /
        NULLIF(COUNT(d.id), 0) * 100,
        2
    ) AS progress_percent
FROM kb_file f
LEFT JOIN kb_document d ON f.id = d.file_id
WHERE f.file_id = 'your-file-id'
GROUP BY f.file_id, f.name;

-- 查看失败的页面
SELECT
    f.name AS file_name,
    d.page_index,
    d.ocr_error,
    d.image_url
FROM kb_document d
JOIN kb_file f ON d.file_id = f.id
WHERE d.ocr_status = 'failed'
ORDER BY f.created_at DESC, d.page_index;
```

---

## 处理流程

### 完整流程图

```
┌─────────────────┐
│  用户上传文件    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  保存文件到S3   │  (FileServiceImpl.upload)
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  判断文件类型                         │
│  - Word: convertWordToPdf()         │
│  - PDF: 直接使用                     │
│  - Image: 直接使用                   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  拆分成页面图片                       │
│  - PDF: splitPdfToImages()          │
│    (Aspose-PDF → 300 DPI JPG)       │
│  - Image: processImageFile()        │
│    (单页处理)                        │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  上传每页图片到S3                     │
│  路径: kb/documents/{fileId}/pages/  │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  创建kb_document记录                 │
│  - 每页一条记录                       │
│  - status: pending                   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  发送OCR任务到RabbitMQ                │
│  - 每页一个消息                       │
│  - 包含: documentId, pageIndex,      │
│          imageUrl, fileType          │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  OCR消费者并发处理                    │
│  - 每个消费者处理一页                  │
│  - 调用AI模型识别图片文字              │
│  - 更新kb_document.content            │
│  - 更新status: completed/failed      │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  检查所有页是否完成                   │
│  - 查询pending/processing页面数       │
│  - 如果=0,则继续                      │
└────────┬────────────────────────────┘
         │ 所有页完成
         ▼
┌─────────────────────────────────────┐
│  聚合OCR内容                          │
│  - 按page_index排序                  │
│  - 拼接所有页content                 │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  更新kb_file.ocr_status: completed   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  发送向量化任务                       │
│  - 使用聚合后的完整内容               │
└─────────────────────────────────────┘
```

### 状态转换

**KbDocument (单页级别)**:
```
pending → processing → completed
                    ↘ failed
```

**KbFile (文件级别)**:
```
pending → (等待所有页完成) → completed
                         ↘ failed (任何页失败)
```

---

## 代码实现

### 1. DocumentProcessService

**核心服务**: 文件格式转换和页面拆分

```java
public interface DocumentProcessService {
    // Word转PDF
    String convertWordToPdf(File wordFile);

    // PDF拆分为图片
    List<String> splitPdfToImages(String pdfFilePath, String fileId);

    // 处理单张图片
    String processImageFile(String imageFilePath, String fileId);

    // 获取PDF页数
    int getPdfPageCount(String pdfFilePath);
}
```

**实现细节**:

```java
@Service
public class DocumentProcessServiceImpl implements DocumentProcessService {

    @Override
    public String convertWordToPdf(File wordFile) {
        // 使用Aspose-Words
        com.aspose.words.Document doc = new com.aspose.words.Document(wordFile.getAbsolutePath());
        File pdfFile = File.createTempFile("converted_", ".pdf");
        doc.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
        return pdfFile.getAbsolutePath();
    }

    @Override
    public List<String> splitPdfToImages(String pdfFilePath, String fileId) {
        List<String> imageUrls = new ArrayList<>();

        // 加载PDF
        Document pdfDocument = new Document(pdfFilePath);
        int pageCount = pdfDocument.getPages().size();

        // 创建JPEG设备 (300 DPI)
        Resolution resolution = new Resolution(300);
        JpegDevice jpegDevice = new JpegDevice(resolution);

        // 处理每一页
        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            File imageFile = File.createTempFile("page_" + pageNumber + "_", ".jpg");

            try (FileOutputStream imageStream = new FileOutputStream(imageFile)) {
                // 转换页面为图片
                jpegDevice.process(
                    pdfDocument.getPages().get_Item(pageNumber),
                    imageStream
                );
            }

            // 上传到S3
            String imageUrl = uploadPageImage(imageFile, fileId, pageNumber);
            imageUrls.add(imageUrl);

            imageFile.delete();
        }

        return imageUrls;
    }

    private String uploadPageImage(File imageFile, String fileId, int pageNumber) {
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            FileInfo fileInfo = fileStorageService.of(fis)
                .setPath("kb/documents/" + fileId + "/pages/")
                .setSaveFilename("page_" + pageNumber + ".jpg")
                .upload();

            return fileInfo.getUrl();
        }
    }
}
```

### 2. OCR消息结构

```java
@Data
@Builder
public class OcrTaskMessage {
    private String taskId;        // 任务ID
    private String fileId;        // 文件ID
    private String documentId;    // 文档ID (页面ID)
    private Integer pageIndex;    // 页码 (1-based)
    private String imageUrl;      // 页面图片URL
    private String fileType;      // 文件类型
}
```

### 3. OCR消费者

```java
@Component
public class OcrConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_OCR)
    public void process(OcrTaskMessage message) {
        // 1. 查找document记录
        KbDocument document = documentMapper.selectOne(
            new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getDocumentId, message.getDocumentId())
        );

        // 2. 更新状态为processing
        document.setOcrStatus(ProcessingStatus.PROCESSING.name());
        documentMapper.updateById(document);

        try {
            // 3. 调用AI进行OCR
            String ocrContent = aiProcessService.performOcr(
                message.getImageUrl(),
                message.getFileType()
            );

            // 4. 保存OCR内容
            document.setContent(ocrContent);
            document.setOcrStatus(ProcessingStatus.COMPLETED.name());
            documentMapper.updateById(document);

            // 5. 检查是否所有页完成
            checkAndTriggerVectorization(document.getFileId());

        } catch (Exception e) {
            // 标记失败
            document.setOcrStatus(ProcessingStatus.FAILED.name());
            document.setOcrError(e.getMessage());
            documentMapper.updateById(document);
        }
    }

    private void checkAndTriggerVectorization(Long fileId) {
        // 查询待处理页面数
        long pendingCount = documentMapper.selectCount(
            new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .in(KbDocument::getOcrStatus, "pending", "processing")
        );

        if (pendingCount == 0) {
            // 所有页完成,聚合内容
            List<KbDocument> documents = documentMapper.selectList(
                new LambdaQueryWrapper<KbDocument>()
                    .eq(KbDocument::getFileId, fileId)
                    .eq(KbDocument::getOcrStatus, "completed")
                    .orderByAsc(KbDocument::getPageIndex)
            );

            StringBuilder fullContent = new StringBuilder();
            for (KbDocument doc : documents) {
                fullContent.append(doc.getContent()).append("\n\n");
            }

            // 触发向量化
            sendVectorizationTask(fileId, fullContent.toString());
        }
    }
}
```

### 4. 文件上传流程

```java
@Service
public class FileServiceImpl implements FileService {

    @Override
    public FileUploadResponse upload(String kbId, MultipartFile file) {
        // 1. 上传原始文件到S3
        FileInfo fileInfo = fileStorageService.of(file)
            .setPath("kb/" + kbId + "/")
            .upload();

        // 2. 保存文件元数据
        KbFile kbFile = KbFile.builder()
            .fileId(IdGenerator.simpleUUID())
            .kbId(Long.parseLong(kbId))
            .name(file.getOriginalFilename())
            .url(fileInfo.getUrl())
            .ocrStatus(ProcessingStatus.PENDING.name())
            .build();

        fileMapper.insert(kbFile);

        // 3. 创建文档页面并发送OCR任务
        createDocumentPages(kbFile, fileInfo.getUrl());

        return FileUploadResponse.builder()
            .fileId(kbFile.getFileId())
            .fileName(kbFile.getName())
            .fileUrl(fileInfo.getUrl())
            .build();
    }

    private void createDocumentPages(KbFile kbFile, String fileUrl) {
        List<String> pageImageUrls = new ArrayList<>();

        // 下载文件
        File tempFile = downloadFileFromUrl(fileUrl);

        // 根据类型处理
        String fileType = kbFile.getType();
        if ("WORD".equalsIgnoreCase(fileType)) {
            // Word → PDF → Images
            String pdfPath = documentProcessService.convertWordToPdf(tempFile);
            pageImageUrls = documentProcessService.splitPdfToImages(pdfPath, kbFile.getFileId());
            new File(pdfPath).delete();

        } else if ("PDF".equalsIgnoreCase(fileType)) {
            // PDF → Images
            pageImageUrls = documentProcessService.splitPdfToImages(
                tempFile.getAbsolutePath(),
                kbFile.getFileId()
            );

        } else if ("IMAGE".equalsIgnoreCase(fileType)) {
            // Single image
            String imageUrl = documentProcessService.processImageFile(
                tempFile.getAbsolutePath(),
                kbFile.getFileId()
            );
            pageImageUrls.add(imageUrl);
        }

        tempFile.delete();

        // 创建document记录并发送OCR任务
        for (int i = 0; i < pageImageUrls.size(); i++) {
            KbDocument document = KbDocument.builder()
                .documentId(IdGenerator.simpleUUID())
                .fileId(kbFile.getId())
                .pageIndex(i + 1)
                .imageUrl(pageImageUrls.get(i))
                .ocrStatus(ProcessingStatus.PENDING.name())
                .build();

            documentMapper.insert(document);

            // 发送OCR任务
            sendOcrTaskForPage(kbFile, document);
        }
    }
}
```

---

## 使用指南

### 上传文件

**API调用**:
```bash
curl -X POST http://localhost:8080/api/knowledge-bases/{kbId}/files \
  -H "Authorization: Bearer {token}" \
  -F "file=@document.pdf"
```

**自动处理流程**:
1. 文件上传到S3: `s3://bucket/kb/{kbId}/files/xxx.pdf`
2. 拆分为页面图片: `s3://bucket/kb/documents/{fileId}/pages/page_1.jpg`
3. 创建30条`kb_document`记录 (假设30页)
4. 发送30个OCR任务到RabbitMQ
5. 并发OCR处理(每页独立)
6. 所有页完成后触发向量化

### 查看处理进度

**API调用**:
```bash
curl http://localhost:8080/api/files/{fileId} \
  -H "Authorization: Bearer {token}"
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "fileId": "file-xxx",
    "name": "document.pdf",
    "totalPages": 30,
    "completedPages": 25,
    "failedPages": 1,
    "ocrStatus": "processing",
    "progress": 83.33,
    "pages": [
      {
        "pageIndex": 1,
        "status": "completed",
        "contentLength": 1250,
        "tokensUsed": 320
      },
      {
        "pageIndex": 2,
        "status": "completed",
        "contentLength": 980,
        "tokensUsed": 250
      },
      {
        "pageIndex": 15,
        "status": "failed",
        "error": "OCR timeout"
      },
      {
        "pageIndex": 26,
        "status": "processing"
      },
      ...
    ]
  }
}
```

### 重试失败的页面

**API调用**:
```bash
curl -X POST http://localhost:8080/api/files/{fileId}/retry-ocr \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "pageIndexes": [15, 20]  // 重试第15页和第20页
  }'
```

**处理逻辑**:
```java
public void retryOcrPages(String fileId, List<Integer> pageIndexes) {
    KbFile file = findByFileId(fileId);

    for (Integer pageIndex : pageIndexes) {
        KbDocument document = documentMapper.selectOne(
            new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, file.getId())
                .eq(KbDocument::getPageIndex, pageIndex)
        );

        if (document != null) {
            // 重置状态
            document.setOcrStatus(ProcessingStatus.PENDING.name());
            document.setOcrError(null);
            documentMapper.updateById(document);

            // 重新发送OCR任务
            sendOcrTaskForPage(file, document);
        }
    }
}
```

### 查看单页OCR结果

**SQL查询**:
```sql
SELECT
    page_index,
    SUBSTRING(content, 1, 100) || '...' AS preview,
    LENGTH(content) AS content_length,
    ocr_status,
    tokens_used,
    image_url
FROM kb_document
WHERE file_id = (SELECT id FROM kb_file WHERE file_id = 'file-xxx')
ORDER BY page_index;
```

**结果**:
```
 page_index |        preview         | content_length | ocr_status | tokens_used
------------+------------------------+----------------+------------+-------------
          1 | 第一章 引言\n\n本文档... |           1250 | completed  |         320
          2 | 1.1 背景\n\n近年来...   |            980 | completed  |         250
          3 | 1.2 目标\n\n本项目...   |           1100 | completed  |         280
         15 | null                   |           null | failed     |           0
         30 | 附录A\n\n参考文献...    |            750 | completed  |         190
```

---

## 故障排查

### 问题1: Word文件转换失败

**错误信息**:
```
BusinessException: Word to PDF conversion failed: License is not set
```

**原因**: Aspose-Words许可证未配置

**解决方案**:
```java
// 在DocumentProcessServiceImpl中添加License设置
static {
    try {
        License license = new License();
        license.setLicense("path/to/Aspose.Words.lic");
    } catch (Exception e) {
        log.warn("Aspose license not set, using evaluation mode");
    }
}
```

### 问题2: PDF拆分图片质量低

**现象**: OCR识别不准确

**原因**: DPI设置过低

**解决方案**:
```java
// 提高分辨率
Resolution resolution = new Resolution(300); // 从150提高到300
JpegDevice jpegDevice = new JpegDevice(resolution);
```

### 问题3: 部分页面OCR失败

**查询失败页面**:
```sql
SELECT
    f.name,
    d.page_index,
    d.ocr_error,
    d.image_url
FROM kb_document d
JOIN kb_file f ON d.file_id = f.id
WHERE d.ocr_status = 'failed'
ORDER BY d.created_at DESC;
```

**常见原因**:
1. 图片损坏 → 重新生成图片
2. AI模型超时 → 调整timeout配置
3. 图片URL过期 → 检查S3签名URL有效期
4. Token超限 → 检查单页内容是否过多

**解决方案**:
```bash
# 重试失败的页面
curl -X POST http://localhost:8080/api/files/{fileId}/retry-ocr \
  -d '{"pageIndexes": [15, 20]}'
```

### 问题4: 向量化未触发

**查询状态**:
```sql
SELECT
    f.file_id,
    f.ocr_status,
    COUNT(d.id) AS total_pages,
    COUNT(CASE WHEN d.ocr_status = 'completed' THEN 1 END) AS completed,
    COUNT(CASE WHEN d.ocr_status = 'failed' THEN 1 END) AS failed,
    COUNT(CASE WHEN d.ocr_status = 'pending' THEN 1 END) AS pending
FROM kb_file f
LEFT JOIN kb_document d ON f.id = d.file_id
WHERE f.file_id = 'file-xxx'
GROUP BY f.file_id, f.ocr_status;
```

**可能原因**:
- 有pending或processing页面 → 等待完成
- 所有页都failed → 需要手动触发或重试

**手动触发向量化**:
```java
// 创建管理API
@PostMapping("/api/admin/files/{fileId}/force-vectorization")
public Result<Void> forceVectorization(@PathVariable String fileId) {
    fileService.forceVectorization(fileId);
    return Result.success();
}
```

### 问题5: 内存不足

**错误信息**:
```
java.lang.OutOfMemoryError: Java heap space
```

**原因**: 大PDF同时处理多页

**解决方案**:

1. **增加堆内存**:
```bash
java -Xmx4g -jar middle-platform.jar
```

2. **分批处理**:
```java
// 限制并发处理页数
@Bean
public ThreadPoolExecutor ocrExecutor() {
    return new ThreadPoolExecutor(
        2,   // 核心线程数
        5,   // 最大线程数
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100)
    );
}
```

3. **清理临时文件**:
```java
// 确保临时文件及时删除
try {
    // ... 处理逻辑
} finally {
    tempFile.delete();
    pdfFile.delete();
}
```

---

## 性能优化

### 1. 并发控制

**RabbitMQ消费者配置**:
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5      # 最小并发消费者
        max-concurrency: 10 # 最大并发消费者
        prefetch: 1         # 每次预取1条消息
```

### 2. 批量查询优化

**使用IN查询代替循环查询**:
```java
// ❌ 错误: N+1查询
for (Long fileId : fileIds) {
    List<KbDocument> docs = documentMapper.selectList(
        new LambdaQueryWrapper<KbDocument>()
            .eq(KbDocument::getFileId, fileId)
    );
}

// ✅ 正确: 批量查询
List<KbDocument> allDocs = documentMapper.selectList(
    new LambdaQueryWrapper<KbDocument>()
        .in(KbDocument::getFileId, fileIds)
);
```

### 3. 缓存页面图片URL

**使用Redis缓存**:
```java
@Cacheable(value = "pageImages", key = "#documentId")
public String getPageImageUrl(String documentId) {
    KbDocument document = documentMapper.selectOne(
        new LambdaQueryWrapper<KbDocument>()
            .eq(KbDocument::getDocumentId, documentId)
    );
    return document != null ? document.getImageUrl() : null;
}
```

---

## 监控指标

### 关键指标

```sql
-- 1. OCR处理速度
SELECT
    DATE_TRUNC('hour', created_at) AS hour,
    COUNT(*) AS total_pages,
    COUNT(CASE WHEN ocr_status = 'completed' THEN 1 END) AS completed,
    ROUND(AVG(tokens_used), 2) AS avg_tokens,
    ROUND(AVG(EXTRACT(EPOCH FROM (updated_at - created_at))), 2) AS avg_seconds
FROM kb_document
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY hour
ORDER BY hour DESC;

-- 2. 失败率统计
SELECT
    DATE(created_at) AS date,
    COUNT(*) AS total,
    COUNT(CASE WHEN ocr_status = 'failed' THEN 1 END) AS failed,
    ROUND(
        COUNT(CASE WHEN ocr_status = 'failed' THEN 1 END)::numeric /
        COUNT(*) * 100,
        2
    ) AS fail_rate
FROM kb_document
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY date
ORDER BY date DESC;

-- 3. Token消耗统计
SELECT
    SUM(tokens_used) AS total_tokens,
    AVG(tokens_used) AS avg_tokens_per_page,
    MAX(tokens_used) AS max_tokens_per_page
FROM kb_document
WHERE ocr_status = 'completed'
  AND created_at > NOW() - INTERVAL '1 day';
```

---

## 总结

### 架构优势

1. ✅ **可扩展性**: 并发处理多个页面,速度更快
2. ✅ **可靠性**: 单页失败不影响其他页,可重试
3. ✅ **可观测性**: 每页状态独立跟踪,进度可视化
4. ✅ **成本优化**: 只重试失败页面,不浪费token
5. ✅ **用户体验**: 实时进度反馈,部分完成也可查看

### 下一步计划

- [ ] 实现页面级别的重试机制
- [ ] 添加OCR质量评分
- [ ] 支持并行下载和转换
- [ ] 优化大文件内存占用
- [ ] 添加处理进度WebSocket推送

---

**文档版本**: v2.0
**最后更新**: 2025-10-04
**维护者**: AI Middle Platform Team
