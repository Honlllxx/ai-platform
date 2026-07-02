package com.platform.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("model_bindings")
public class ModelBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bindingName;
    private Long baseModelId;
    private Long loraAdapterId;
    private Long datasetId;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
