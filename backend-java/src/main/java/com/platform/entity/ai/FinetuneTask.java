package com.platform.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("finetune_tasks")
public class FinetuneTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String baseModel;
    private String datasetName;
    private Integer datasetSize;
    private String status;
    private Integer progress;
    private Integer epochs;
    private Integer currentEpoch;
    private Double learningRate;
    private Double loss;
    private String metrics;
    private String outputModel;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Double duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
