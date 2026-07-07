package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.PipelineResultMessage;
import com.platform.entity.document.Document;
import com.platform.entity.pipeline.Pipeline;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Consumes pipeline processing results from Python AI service via RocketMQ.
 *
 * Flow: Python AI finishes processing → RocketMQ (pipeline-result)
 *       → this consumer → updates Document/Pipeline status in DB
 *
 * This replaces the old pattern where the Java consumer called Python via HTTP.
 * Now Python is the primary pipeline executor; Java only updates business state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "pipeline-result",
    consumerGroup = "platform-result-consumer-group",
    selectorExpression = "result"
)
public class PipelineResultConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final PipelineMapper pipelineMapper;
    private final DocumentMapper documentMapper;

    @Override
    public void onMessage(String messageBody) {
        PipelineResultMessage result = null;
        try {
            result = objectMapper.readValue(messageBody, PipelineResultMessage.class);
            log.info("Received pipeline result: taskId={}, documentId={}, status={}",
                    result.getTaskId(), result.getDocumentId(), result.getStatus());

            // Update Document status
            if (result.getDocumentId() != null) {
                Document doc = documentMapper.selectById(result.getDocumentId());
                if (doc != null) {
                    doc.setStatus(result.getStatus());
                    if (result.getErrorMessage() != null) {
                        doc.setErrorMessage(result.getErrorMessage());
                    }
                    documentMapper.updateById(doc);
                    log.info("Document {} status updated → {}", result.getDocumentId(), result.getStatus());
                }
            }

            // Update Pipeline status
            if (result.getPipelineId() != null) {
                Pipeline pipeline = pipelineMapper.selectById(result.getPipelineId());
                if (pipeline != null) {
                    pipeline.setStatus(result.getStatus());
                    if (result.getDuration() != null) {
                        pipeline.setDuration(result.getDuration());
                    }
                    if (result.getErrorMessage() != null) {
                        pipeline.setErrorMessage(result.getErrorMessage());
                    }
                    pipeline.setFinishedAt(LocalDateTime.now());
                    pipelineMapper.updateById(pipeline);
                    log.info("Pipeline {} status updated → {}", result.getPipelineId(), result.getStatus());
                }
            }

        } catch (Exception e) {
            log.error("Failed to process pipeline result: documentId={}, error={}",
                    result != null ? result.getDocumentId() : "unknown", e.getMessage(), e);
        }
    }
}
