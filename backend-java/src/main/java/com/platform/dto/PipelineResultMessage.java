package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RocketMQ message payload for pipeline processing results.
 * Python produces this message after completing AI pipeline processing;
 * Java consumes it to update Document/Pipeline status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineResultMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId;
    private Long documentId;
    private Long pipelineId;
    private String status;          // "completed" | "failed"
    private Double duration;        // processing time in seconds
    private Integer extractionCount;
    private String errorMessage;
}
