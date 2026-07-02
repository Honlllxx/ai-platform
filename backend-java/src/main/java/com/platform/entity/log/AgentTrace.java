package com.platform.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_traces")
public class AgentTrace {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String sessionId;
    private Long pipelineId;
    private Long userId;
    private String username;
    private String query;
    private String status;
    private Double totalDuration;
    private Integer totalTokens;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
