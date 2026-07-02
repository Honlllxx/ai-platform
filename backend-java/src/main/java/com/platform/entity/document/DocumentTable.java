package com.platform.entity.document;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_tables")
public class DocumentTable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Double confidence;
    private String originalTable;
    private String extractedTable;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
