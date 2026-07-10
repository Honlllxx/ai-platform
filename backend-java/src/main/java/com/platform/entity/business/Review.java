package com.platform.entity.business;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reviews")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long pipelineId;
    private String documentType;
    private String status;
    private Integer totalFields;
    private Integer reviewedFields;
    private Integer approvedFields;
    private Integer rejectedFields;
    private Long reviewerId;
    private LocalDateTime startedAt;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
