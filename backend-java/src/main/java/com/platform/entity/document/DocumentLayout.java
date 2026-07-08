package com.platform.entity.document;

import com.baomidou.mybatisplus.annotation.*;
import com.platform.handler.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_layouts")
public class DocumentLayout {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String imageUrl;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String regions;
    private Double confidence;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
