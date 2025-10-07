package com.ai.middle.platform.mq.consumer;

import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.config.RabbitMQConfig;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.mq.message.KgGenerationTaskMessage;
import com.ai.middle.platform.mq.message.QaGenerationTaskMessage;
import com.ai.middle.platform.mq.message.VectorizationTaskMessage;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.service.AIProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 向量化消息消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorizationConsumer {

    private final FileDetailMapper fileDetailMapper;
    private final AIProcessService aiProcessService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_VECTORIZATION)
    public void process(VectorizationTaskMessage message) {
        log.info("收到向量化任务: {}", message);

        try {
            updateFileStatus(message.getFileId(), ProcessingStatus.PROCESSING, null);

            aiProcessService.performVectorization(
                    message.getFileId(),
                    message.getOcrContent(),
                    message.getChunkSize(),
                    message.getOverlap()
            );

            updateFileStatus(message.getFileId(), ProcessingStatus.COMPLETED, null);

            triggerQaGenerationTask(message.getFileId(), message.getOcrContent());
            triggerKgGenerationTask(message.getFileId(), message.getOcrContent());

            log.info("向量化任务完成: fileId={}", message.getFileId());
        } catch (Exception e) {
            log.error("向量化任务失败: fileId={}", message.getFileId(), e);
            updateFileStatus(message.getFileId(), ProcessingStatus.FAILED, e.getMessage());
        }
    }

    private void updateFileStatus(String fileId, ProcessingStatus status, String errorMessage) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            return;
        }
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        attributes.setVectorizationStatus(status.getCode());
        if (status == ProcessingStatus.FAILED) {
            attributes.setErrorMessage(errorMessage);
        } else if (status == ProcessingStatus.COMPLETED) {
            attributes.setErrorMessage(null);
        }
        fileDetail.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(fileDetail);
    }

    private void triggerQaGenerationTask(String fileId, String ocrContent) {
        QaGenerationTaskMessage taskMessage = QaGenerationTaskMessage.builder()
                .taskId(cn.hutool.core.util.IdUtil.simpleUUID())
                .fileId(fileId)
                .ocrContent(ocrContent)
                .maxPairs(50)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_QA,
                RabbitMQConfig.ROUTING_KEY_QA,
                taskMessage
        );
    }

    private void triggerKgGenerationTask(String fileId, String ocrContent) {
        KgGenerationTaskMessage taskMessage = KgGenerationTaskMessage.builder()
                .taskId(cn.hutool.core.util.IdUtil.simpleUUID())
                .fileId(fileId)
                .ocrContent(ocrContent)
                .extractEntities(Boolean.TRUE)
                .extractRelations(Boolean.TRUE)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_KG,
                RabbitMQConfig.ROUTING_KEY_KG,
                taskMessage
        );
    }
}
