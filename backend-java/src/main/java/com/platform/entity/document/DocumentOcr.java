package com.platform.entity.document;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_ocr")
public class DocumentOcr {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Integer totalChars;
    private Double avgConfidence;
    private String language;
    private String entries;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
