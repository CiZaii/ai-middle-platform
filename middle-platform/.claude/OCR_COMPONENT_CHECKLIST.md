# OCR分页处理 - 组件清单

> **版本**: v2.0
> **检查日期**: 2025-10-04
> **状态**: ✅ 所有组件已就绪

---

## ✅ 已创建的组件清单

### 1. 数据库表

#### kb_document (文档页面表)
**文件位置**: `src/main/resources/sql/init.sql`

**状态**: ✅ 已创建

**表结构**:
```sql
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
```

**索引**:
- ✅ `idx_kb_document_file_id` - 快速查找文件的所有页面
- ✅ `idx_kb_document_status` - 快速查找待处理页面
- ✅ `UNIQUE(file_id, page_index)` - 确保同一文件的页码唯一

**触发器**:
- ✅ `trigger_update_kb_document_updated_at` - 自动更新updated_at

---

### 2. Entity层

#### KbDocument.java
**文件位置**: `src/main/java/com/ai/middle/platform/entity/po/KbDocument.java`

**状态**: ✅ 已创建

**关键字段**:
```java
@Data
@Builder
@TableName("kb_document")
public class KbDocument {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String documentId;    // 文档ID (UUID)
    private Long fileId;          // 关联的文件ID
    private Integer pageIndex;    // 页码 (1-based)
    private String content;       // OCR内容
    private String imageUrl;      // 页面图片URL
    private String ocrStatus;     // pending/processing/completed/failed
    private Integer tokensUsed;   // 消耗的token数
    private String ocrError;      // 错误信息

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**注解**:
- ✅ `@TableName("kb_document")` - 映射到表
- ✅ `@TableId` - 主键自增
- ✅ `@TableField(fill = FieldFill.INSERT_UPDATE)` - 自动填充时间

---

### 3. Mapper层

#### KbDocumentMapper.java
**文件位置**: `src/main/java/com/ai/middle/platform/repository/mapper/KbDocumentMapper.java`

**状态**: ✅ 已创建

**代码**:
```java
@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {
}
```

**继承方法** (来自BaseMapper):
- ✅ `insert(KbDocument)` - 插入记录
- ✅ `selectById(Long)` - 按ID查询
- ✅ `selectOne(Wrapper)` - 单条查询
- ✅ `selectList(Wrapper)` - 列表查询
- ✅ `selectCount(Wrapper)` - 计数查询
- ✅ `updateById(KbDocument)` - 更新记录
- ✅ `deleteById(Long)` - 删除记录

---

### 4. Service层

#### DocumentProcessService.java (接口)
**文件位置**: `src/main/java/com/ai/middle/platform/service/DocumentProcessService.java`

**状态**: ✅ 已创建

**方法定义**:
```java
public interface DocumentProcessService {

    // Word转PDF
    String convertWordToPdf(File wordFile);

    // PDF拆分为图片 (每页一张JPG)
    List<String> splitPdfToImages(String pdfFilePath, String fileId);

    // 处理单张图片
    String processImageFile(String imageFilePath, String fileId);

    // 获取PDF页数
    int getPdfPageCount(String pdfFilePath);
}
```

#### DocumentProcessServiceImpl.java (实现)
**文件位置**: `src/main/java/com/ai/middle/platform/service/impl/DocumentProcessServiceImpl.java`

**状态**: ✅ 已创建

**核心功能**:
1. ✅ **Word转PDF**
   - 使用Aspose-Words
   - 生成临时PDF文件
   - 返回PDF路径

2. ✅ **PDF拆分为图片**
   - 使用Aspose-PDF
   - 300 DPI高清晰度
   - 每页生成JPG
   - 上传到S3: `kb/documents/{fileId}/pages/page_N.jpg`

3. ✅ **图片处理**
   - 单张图片上传
   - 返回S3 URL

**依赖**:
- ✅ `FileStorageService` - x-file-storage文件上传
- ✅ Aspose-Words - Word文档处理
- ✅ Aspose-PDF - PDF文档处理

---

### 5. MQ消息层

#### OcrTaskMessage.java
**文件位置**: `src/main/java/com/ai/middle/platform/mq/message/OcrTaskMessage.java`

**状态**: ✅ 已更新

**新字段**:
```java
@Data
@Builder
public class OcrTaskMessage {
    private String taskId;
    private String fileId;

    // 新增字段 (v2.0)
    private String documentId;    // 文档ID (页面唯一标识)
    private Integer pageIndex;    // 页码 (1-based)
    private String imageUrl;      // 页面图片URL

    private String fileType;

    @Deprecated
    private String filePath;      // 保留兼容性
}
```

---

### 6. Consumer层

#### OcrConsumer.java
**文件位置**: `src/main/java/com/ai/middle/platform/mq/consumer/OcrConsumer.java`

**状态**: ✅ 已重构

**核心逻辑**:
```java
@RabbitListener(queues = RabbitMQConfig.QUEUE_OCR)
public void process(OcrTaskMessage message) {
    // 1. 查找document记录
    KbDocument document = documentMapper.selectOne(...);

    // 2. 更新状态: pending → processing
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
```

**新增方法**:
- ✅ `checkAndTriggerVectorization()` - 检查所有页是否完成
- ✅ `updateFileStatus()` - 更新文件级别状态
- ✅ `sendVectorizationTask()` - 触发向量化

---

### 7. FileService层

#### FileServiceImpl.java
**文件位置**: `src/main/java/com/ai/middle/platform/service/impl/FileServiceImpl.java`

**状态**: ✅ 已更新

**新增方法**:
```java
// 创建文档页面
private void createDocumentPages(KbFile kbFile, String fileUrl) {
    // 1. 下载文件
    File tempFile = downloadFileFromUrl(fileUrl);

    // 2. 根据类型处理
    List<String> pageImageUrls;
    if (fileType == WORD) {
        String pdfPath = documentProcessService.convertWordToPdf(tempFile);
        pageImageUrls = documentProcessService.splitPdfToImages(pdfPath, fileId);
    } else if (fileType == PDF) {
        pageImageUrls = documentProcessService.splitPdfToImages(tempFile, fileId);
    } else if (fileType == IMAGE) {
        pageImageUrls = List.of(documentProcessService.processImageFile(tempFile, fileId));
    }

    // 3. 创建document记录
    for (int i = 0; i < pageImageUrls.size(); i++) {
        KbDocument document = KbDocument.builder()
            .documentId(UUID)
            .fileId(fileId)
            .pageIndex(i + 1)
            .imageUrl(pageImageUrls.get(i))
            .ocrStatus(PENDING)
            .build();
        documentMapper.insert(document);

        // 4. 发送OCR任务
        sendOcrTaskForPage(kbFile, document);
    }
}

// 发送单页OCR任务
private void sendOcrTaskForPage(KbFile file, KbDocument document) {
    OcrTaskMessage message = OcrTaskMessage.builder()
        .taskId(UUID)
        .fileId(file.getFileId())
        .documentId(document.getDocumentId())
        .pageIndex(document.getPageIndex())
        .imageUrl(document.getImageUrl())
        .fileType(file.getType())
        .build();

    rabbitTemplate.convertAndSend(OCR_QUEUE, message);
}
```

---

## 📊 依赖关系图

```
FileServiceImpl
    ↓
DocumentProcessService (接口)
    ↓
DocumentProcessServiceImpl (实现)
    ↓
    ├── Aspose-Words (Word → PDF)
    ├── Aspose-PDF (PDF → Images)
    └── FileStorageService (上传到S3)

KbDocument (Entity)
    ↓
KbDocumentMapper (Mapper)
    ↓
OcrConsumer
    ↓
AIProcessService (OCR识别)
```

---

## 🔍 验证检查清单

### 数据库层
- [x] kb_document表已创建
- [x] 索引已创建 (file_id, ocr_status)
- [x] 唯一约束已创建 (file_id, page_index)
- [x] 触发器已创建 (updated_at自动更新)

### 实体层
- [x] KbDocument.java 存在
- [x] 所有字段已定义
- [x] MyBatis-Plus注解正确
- [x] Builder模式支持

### Mapper层
- [x] KbDocumentMapper.java 存在
- [x] @Mapper注解存在
- [x] 继承BaseMapper<KbDocument>

### Service层
- [x] DocumentProcessService接口存在
- [x] DocumentProcessServiceImpl实现存在
- [x] convertWordToPdf方法实现
- [x] splitPdfToImages方法实现
- [x] processImageFile方法实现
- [x] FileStorageService注入

### MQ层
- [x] OcrTaskMessage更新 (新增documentId, pageIndex, imageUrl)
- [x] OcrConsumer重构 (单页处理逻辑)
- [x] checkAndTriggerVectorization实现
- [x] 聚合内容逻辑实现

### 文件处理层
- [x] FileServiceImpl.createDocumentPages实现
- [x] FileServiceImpl.sendOcrTaskForPage实现
- [x] 支持Word → PDF转换
- [x] 支持PDF → Images拆分
- [x] 支持Image直接处理

---

## 🚀 使用示例

### 1. 上传文件触发分页OCR

```bash
curl -X POST http://localhost:8080/api/knowledge-bases/{kbId}/files \
  -H "Authorization: Bearer {token}" \
  -F "file=@document.pdf"
```

**自动流程**:
1. 文件上传到S3
2. PDF拆分为30张图片 (假设30页)
3. 创建30条kb_document记录
4. 发送30个OCR任务
5. 并发OCR处理
6. 所有页完成后触发向量化

### 2. 查询文件的所有页面

```java
List<KbDocument> pages = kbDocumentMapper.selectList(
    new LambdaQueryWrapper<KbDocument>()
        .eq(KbDocument::getFileId, fileId)
        .orderByAsc(KbDocument::getPageIndex)
);
```

### 3. 查询待处理的页面

```java
long pendingCount = kbDocumentMapper.selectCount(
    new LambdaQueryWrapper<KbDocument>()
        .eq(KbDocument::getFileId, fileId)
        .in(KbDocument::getOcrStatus, "pending", "processing")
);
```

### 4. 获取OCR完成的内容

```java
List<KbDocument> completedPages = kbDocumentMapper.selectList(
    new LambdaQueryWrapper<KbDocument>()
        .eq(KbDocument::getFileId, fileId)
        .eq(KbDocument::getOcrStatus, "completed")
        .orderByAsc(KbDocument::getPageIndex)
);

StringBuilder fullContent = new StringBuilder();
for (KbDocument page : completedPages) {
    fullContent.append(page.getContent()).append("\n\n");
}
```

---

## ⚠️ 注意事项

### 1. Aspose许可证

确保Aspose-Words和Aspose-PDF的许可证已配置:

```java
// DocumentProcessServiceImpl.java 添加静态初始化块
static {
    try {
        // Aspose-Words License
        com.aspose.words.License wordsLicense = new com.aspose.words.License();
        wordsLicense.setLicense("path/to/Aspose.Words.lic");

        // Aspose-PDF License
        com.aspose.pdf.License pdfLicense = new com.aspose.pdf.License();
        pdfLicense.setLicense("path/to/Aspose.PDF.lic");

    } catch (Exception e) {
        log.warn("Aspose license not set, using evaluation mode");
    }
}
```

### 2. x-file-storage配置

确保S3配置正确:

```yaml
dromara:
  x-file-storage:
    default-platform: s3
    s3:
      - platform: s3
        enable-storage: true
        access-key: ${S3_ACCESS_KEY}
        secret-key: ${S3_SECRET_KEY}
        end-point: ${S3_ENDPOINT}
        bucket-name: ${S3_BUCKET}
        domain: ${S3_DOMAIN}
```

### 3. RabbitMQ配置

确保OCR队列已配置:

```java
public static final String QUEUE_OCR = "kb.ocr.queue";
public static final String EXCHANGE_OCR = "kb.ocr.exchange";
public static final String ROUTING_KEY_OCR = "kb.ocr";
```

---

## ✅ 总结

**所有必要的组件都已创建并就绪!**

你现在拥有:
- ✅ 完整的数据库表结构
- ✅ Entity和Mapper层
- ✅ 文档处理服务 (Word/PDF/Image)
- ✅ OCR消息和消费者
- ✅ 文件上传集成

**下一步**:
1. 执行SQL迁移脚本创建kb_document表
2. 配置Aspose许可证
3. 测试文件上传和分页OCR流程

**文档位置**:
- 详细使用指南: `.claude/OCR_PAGE_PROCESSING_GUIDE.md`
- 组件清单: `.claude/OCR_COMPONENT_CHECKLIST.md` (本文档)

---

**检查完成时间**: 2025-10-04
**检查人**: Claude Code
**状态**: ✅ 所有组件已就绪,可以开始使用
