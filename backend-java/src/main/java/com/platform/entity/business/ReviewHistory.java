package com.platform.entity.business;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_history")
public class ReviewHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long fieldId;
    private String action;
    private Long operatorId;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
