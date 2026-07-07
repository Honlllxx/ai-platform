package com.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.PipelineTaskMessage;
import com.platform.entity.document.Document;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * [FALLBACK] Java-side pipeline consumer — calls Python AI via HTTP.
 *
 * This consumer is in a separate group from the Python RocketMQ consumer.
 * When the Python consumer is active (normal operation), this consumer
 * will NOT receive messages because Python is in the primary consumer group.
 *
 * If Python is down and you need Java to bridge via HTTP, change the
 * consumerGroup to a different value and restart.
 *
 * Primary flow: RocketMQ → Python Consumer → LangGraph → Python Producer
 *               → pipeline-result → PipelineResultConsumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "pipeline-task",
    consumerGroup = "platform-consumer-group-fallback",
    selectorExpression = "pipeline"
)
public class PipelineTaskConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final AiPipelineService aiPipelineService;
    private final PipelineMapper pipelineMapper;
    private final DocumentMapper documentMapper;
    private final ExtractionResultMapper extractionResultMapper;

    @Override
    public void onMessage(String messageBody) {
        PipelineTaskMessage task = null;
        try {
            task = objectMapper.readValue(messageBody, PipelineTaskMessage.class);
            log.info("Received pipeline task: taskId={}, documentId={}", task.getTaskId(), task.getDocumentId());

            // Update pipeline status to processing
            if (task.getPipelineId() != null) {
                Pipeline pipeline = pipelineMapper.selectById(task.getPipelineId());
                if (pipeline != null) {
                    pipeline.setStatus("processing");
                    pipelineMapper.updateById(pipeline);
                }
            }

            // Update document status
            Document doc = documentMapper.selectById(task.getDocumentId());
            if (doc != null) {
                doc.setStatus("processing");
                documentMapper.updateById(doc);
            }

            // Call Python AI service
            Map<String, Object> result = aiPipelineService.runPipeline(
                    task.getDocumentId(), task.getMetadataFields());

            log.info("Pipeline task completed: taskId={}, status={}, duration={}",
                    task.getTaskId(), result.get("status"), result.get("duration"));

        } catch (Exception e) {
            log.error("Pipeline task failed: documentId={}, error={}",
                    task != null ? task.getDocumentId() : "unknown", e.getMessage(), e);

            // Mark pipeline as failed
            if (task != null && task.getPipelineId() != null) {
                Pipeline pipeline = pipelineMapper.selectById(task.getPipelineId());
                if (pipeline != null) {
                    pipeline.setStatus("failed");
                    pipeline.setErrorMessage(e.getMessage());
                    pipeline.setFinishedAt(LocalDateTime.now());
                    pipelineMapper.updateById(pipeline);
                }
            }

            // Mark document as failed
            if (task != null) {
                Document doc = documentMapper.selectById(task.getDocumentId());
                if (doc != null) {
                    doc.setStatus("failed");
                    doc.setErrorMessage(e.getMessage());
                    documentMapper.updateById(doc);
                }
            }
        }
    }
}
