package com.platform.entity.log;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_trace_nodes")
public class AgentTraceNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long traceId;
    private String nodeId;
    private String parentNodeId;
    private String name;
    private String type;
    private String status;
    private String input;
    private String output;
    private Integer tokenCount;
    private String modelName;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Double duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
