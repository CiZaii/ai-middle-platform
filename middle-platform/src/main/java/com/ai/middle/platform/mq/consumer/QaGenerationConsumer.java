package com.ai.middle.platform.mq.consumer;

import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.config.RabbitMQConfig;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.mq.message.QaGenerationTaskMessage;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.service.AIProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 问答对生成消息消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QaGenerationConsumer {

    private final FileDetailMapper fileDetailMapper;
    private final AIProcessService aiProcessService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_QA_GENERATION)
    public void process(QaGenerationTaskMessage message) {
        log.info("收到问答对生成任务: {}", message);

        try {
            updateFileStatus(message.getFileId(), ProcessingStatus.PROCESSING, null);

            aiProcessService.generateQaPairs(
                    message.getFileId(),
                    message.getOcrContent(),
                    message.getMaxPairs()
            );

            updateFileStatus(message.getFileId(), ProcessingStatus.COMPLETED, null);

            log.info("问答对生成任务完成: fileId={}", message.getFileId());
        } catch (Exception e) {
            log.error("问答对生成任务失败: fileId={}", message.getFileId(), e);
            updateFileStatus(message.getFileId(), ProcessingStatus.FAILED, e.getMessage());
        }
    }

    private void updateFileStatus(String fileId, ProcessingStatus status, String errorMessage) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            return;
        }
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        attributes.setQaPairsStatus(status.getCode());
        if (status == ProcessingStatus.FAILED) {
            attributes.setErrorMessage(errorMessage);
        } else if (status == ProcessingStatus.COMPLETED) {
            attributes.setErrorMessage(null);
        }
        fileDetail.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(fileDetail);
    }
}
