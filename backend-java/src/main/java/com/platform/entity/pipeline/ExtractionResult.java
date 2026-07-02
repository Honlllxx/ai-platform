package com.platform.entity.pipeline;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("extraction_results")
public class ExtractionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pipelineId;
    private String fieldName;
    private String fieldCode;
    private String extractedValue;
    private Double confidence;
    private String status;
    private Integer pageRef;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
