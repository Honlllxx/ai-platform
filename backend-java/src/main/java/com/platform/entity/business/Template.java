package com.platform.entity.business;

import com.baomidou.mybatisplus.annotation.*;
import com.platform.handler.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("templates")
public class Template {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String documentType;
    private String description;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String regions;
    private Integer status;
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
