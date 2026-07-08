package com.platform.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.platform.handler.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("evaluations")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private Long promptVersionId;
    private String promptVersionName;
    private String datasetName;
    private Integer datasetSize;
    private Double accuracy;
    private Double precisionVal;
    private Double recall;
    private Double f1Score;
    private String status;
    private Double duration;
    private String errorMessage;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String detail;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
