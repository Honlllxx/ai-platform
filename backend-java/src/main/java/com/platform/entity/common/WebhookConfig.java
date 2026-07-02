package com.platform.entity.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("webhook_config")
public class WebhookConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String secret;
    private Boolean enabled;
    private String events;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
