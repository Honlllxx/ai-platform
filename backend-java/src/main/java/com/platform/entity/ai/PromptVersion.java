package com.platform.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prompt_versions")
public class PromptVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String version;
    private String documentType;
    private String stage;
    private String content;
    private String variables;
    private String author;
    private String status;
    private String commitMessage;
    private String gitCommitHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
