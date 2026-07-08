package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ message payload for pipeline task submission.
 * Java produces this message; an async consumer picks it up and calls the Python AI service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long documentId;
    private Long pipelineId;
    private String taskId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private List<Map<String, String>> metadataFields;
    private Long userId;
    private String timestamp;
}
