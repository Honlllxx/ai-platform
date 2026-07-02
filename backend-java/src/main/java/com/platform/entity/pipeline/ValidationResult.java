package com.platform.entity.pipeline;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("validation_results")
public class ValidationResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pipelineId;
    private String rule;
    private String fieldName;
    private String severity;
    private String message;
    private String suggestion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
