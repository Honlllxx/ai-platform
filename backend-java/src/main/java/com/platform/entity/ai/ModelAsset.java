package com.platform.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("model_assets")
public class ModelAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String version;
    private String type;
    private String description;
    private String size;
    private String filePath;
    private String framework;
    private String status;
    private String metrics;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
