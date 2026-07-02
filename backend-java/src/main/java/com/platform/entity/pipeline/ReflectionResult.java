package com.platform.entity.pipeline;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reflection_results")
public class ReflectionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pipelineId;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
