package com.ai.middle.platform.mq.consumer;

import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.config.RabbitMQConfig;

import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.entity.po.KbDocument;
import com.ai.middle.platform.mq.message.OcrTaskMessage;
import com.ai.middle.platform.mq.message.VectorizationTaskMessage;

import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.KbDocumentMapper;
import com.ai.middle.platform.service.AIProcessService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrConsumer {

    private final AIProcessService aiProcessService;
    private final KbDocumentMapper documentMapper;
    private final FileDetailMapper fileDetailMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_OCR)
    public void process(OcrTaskMessage message) {
        log.info("Received OCR task: documentId={}, pageIndex={}", message.getDocumentId(), message.getPageIndex());

        KbDocument document = documentMapper.selectOne(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getDocumentId, message.getDocumentId()));
        if (document == null) {
            log.error("Document not found for OCR task: {}", message.getDocumentId());
            return;
        }

        try {
            document.setOcrStatus(ProcessingStatus.PROCESSING.getCode());
            document.setOcrError(null);
            documentMapper.updateById(document);

            updateFileStatus(document.getFileId(), ProcessingStatus.PROCESSING, null);

            String ocrContent = aiProcessService.performOcr(message.getImageUrl(), message.getFileType());

            document.setContent(ocrContent);
            document.setOcrStatus(ProcessingStatus.COMPLETED.getCode());
            document.setOcrError(null);
            documentMapper.updateById(document);

            log.info("OCR completed for documentId={} pageIndex={} characters={}",
                    message.getDocumentId(), message.getPageIndex(),
                    ocrContent != null ? ocrContent.length() : 0);

            checkAndTriggerVectorization(document.getFileId());
        } catch (Exception ex) {
            log.error("OCR failed for documentId={}", message.getDocumentId(), ex);
            document.setOcrStatus(ProcessingStatus.FAILED.getCode());
            document.setOcrError(ex.getMessage());
            documentMapper.updateById(document);
            updateFileStatus(document.getFileId(), ProcessingStatus.FAILED, ex.getMessage());
        }
    }

    private void checkAndTriggerVectorization(String fileId) {
        long pendingCount = documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .in(KbDocument::getOcrStatus,
                        ProcessingStatus.PENDING.getCode(),
                        ProcessingStatus.PROCESSING.getCode()));

        if (pendingCount > 0) {
            return;
        }

        List<KbDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .eq(KbDocument::getOcrStatus, ProcessingStatus.COMPLETED.getCode())
                .orderByAsc(KbDocument::getPageIndex));

        if (documents.isEmpty()) {
            log.warn("All documents failed for fileId={}, skipping vectorization trigger", fileId);
            updateFileStatus(fileId, ProcessingStatus.FAILED, "OCR failed for all pages");
            return;
        }

        StringBuilder fullContent = new StringBuilder();
        for (KbDocument doc : documents) {
            if (doc.getContent() != null) {
                fullContent.append(doc.getContent()).append("\n\n");
            }
        }

        String aggregatedContent = fullContent.toString().trim();
        updateFileStatus(fileId, ProcessingStatus.COMPLETED, null);

        FileDetail file = fileDetailMapper.selectById(fileId);
        if (file == null) {
            log.warn("File not found for OCR completion: {}", fileId);
            return;
        }

        sendVectorizationTask(file.getId(), aggregatedContent);
    }

    private void updateFileStatus(String fileId, ProcessingStatus status, String errorMessage) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            log.warn("Attempted to update status for missing file: {}", fileId);
            return;
        }
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        attributes.setOcrStatus(status.getCode());
        if (status == ProcessingStatus.FAILED) {
            attributes.setErrorMessage(errorMessage);
        } else if (status == ProcessingStatus.COMPLETED) {
            attributes.setErrorMessage(null);
        }
        fileDetail.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(fileDetail);
    }

    private void sendVectorizationTask(String fileId, String content) {
        VectorizationTaskMessage message = VectorizationTaskMessage.builder()
                .taskId(IdGenerator.simpleUUID())
                .fileId(fileId)
                .ocrContent(content)
                .chunkSize(1000)
                .overlap(200)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_VECTORIZATION,
                RabbitMQConfig.ROUTING_KEY_VECTORIZATION,
                message
        );

        log.info("Triggered vectorization for fileId={} contentLength={}", fileId, content != null ? content.length() : 0);
    }
}
