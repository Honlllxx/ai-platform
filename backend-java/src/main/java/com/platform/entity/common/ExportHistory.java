package com.platform.entity.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("export_history")
public class ExportHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String format;
    private String fileUrl;
    private String documentIds;
    private Integer recordCount;
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
