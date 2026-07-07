package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.PipelineTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Sends pipeline tasks to RocketMQ for async processing.
 *
 * Flow: Document uploaded → send task message → response returns immediately
 *       → consumer picks up message → calls Python AI → persists results
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RocketMQProducerService {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    private static final String PIPELINE_TOPIC = "pipeline-task";
    private static final String PIPELINE_TAG = "pipeline";

    /**
     * Submit a pipeline task to the async queue.
     * Returns immediately with the task ID — the consumer will process asynchronously.
     */
    public String submitPipelineTask(PipelineTaskMessage task) {
        try {
            String payload = objectMapper.writeValueAsString(task);
            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader("taskId", task.getTaskId())
                    .setHeader("documentId", task.getDocumentId())
                    .build();

            SendResult result = rocketMQTemplate.syncSend(
                    PIPELINE_TOPIC + ":" + PIPELINE_TAG, message, 5000);

            log.info("Pipeline task submitted: taskId={}, documentId={}, msgId={}",
                    task.getTaskId(), task.getDocumentId(), result.getMsgId());
            return task.getTaskId();

        } catch (Exception e) {
            log.error("Failed to submit pipeline task: documentId={}, error={}",
                    task.getDocumentId(), e.getMessage(), e);
            throw new RuntimeException("Failed to submit async pipeline task: " + e.getMessage(), e);
        }
    }
}
