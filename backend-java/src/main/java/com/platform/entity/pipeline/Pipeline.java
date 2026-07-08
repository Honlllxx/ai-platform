package com.platform.entity.pipeline;

import com.baomidou.mybatisplus.annotation.*;
import com.platform.handler.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pipelines")
public class Pipeline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String taskId;
    private String status;
    private String currentStep;
    private Integer totalSteps;
    private Integer completedSteps;
    private Double duration;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String stateGraph;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
