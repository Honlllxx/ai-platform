package com.platform.entity.business;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_fields")
public class ReviewField {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long extractionId;
    private String fieldName;
    private String fieldCode;
    private String extractedValue;
    private Double confidence;
    private String imageRegion;
    private String status;
    private String correctedValue;
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
