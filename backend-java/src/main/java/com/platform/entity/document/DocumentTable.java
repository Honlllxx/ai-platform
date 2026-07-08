package com.platform.entity.document;

import com.baomidou.mybatisplus.annotation.*;
import com.platform.handler.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_tables")
public class DocumentTable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Double confidence;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String originalTable;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String extractedTable;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
