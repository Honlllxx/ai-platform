package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.log.AgentTrace;
import com.platform.entity.log.AgentTraceNode;
import com.platform.mapper.log.AgentTraceMapper;
import com.platform.mapper.log.AgentTraceNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent-traces")
@RequiredArgsConstructor
public class AgentTraceController {

    private final AgentTraceMapper agentTraceMapper;
    private final AgentTraceNodeMapper agentTraceNodeMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_log:agent')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<AgentTrace> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(AgentTrace::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AgentTrace::getTraceId, keyword)
                    .or().like(AgentTrace::getUsername, keyword));
        }
        wrapper.orderByDesc(AgentTrace::getCreatedAt);

        Page<AgentTrace> tracePage = agentTraceMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> records = tracePage.getRecords().stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("traceId", t.getTraceId());
            map.put("sessionId", t.getSessionId());
            map.put("pipelineId", t.getPipelineId());
            map.put("userId", t.getUserId());
            map.put("username", t.getUsername());
            map.put("query", t.getQuery());
            map.put("status", t.getStatus());
            map.put("totalDuration", t.getTotalDuration());
            map.put("totalTokens", t.getTotalTokens());
            map.put("startedAt", t.getStartedAt());
            map.put("finishedAt", t.getFinishedAt());
            map.put("createTime", t.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, tracePage.getTotal(), tracePage.getCurrent(), tracePage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_log:agent')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        AgentTrace trace = agentTraceMapper.selectById(id);
        if (trace == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", trace.getId());
        map.put("traceId", trace.getTraceId());
        map.put("sessionId", trace.getSessionId());
        map.put("pipelineId", trace.getPipelineId());
        map.put("userId", trace.getUserId());
        map.put("username", trace.getUsername());
        map.put("query", trace.getQuery());
        map.put("status", trace.getStatus());
        map.put("totalDuration", trace.getTotalDuration());
        map.put("totalTokens", trace.getTotalTokens());
        map.put("startedAt", trace.getStartedAt());
        map.put("finishedAt", trace.getFinishedAt());
        map.put("createTime", trace.getCreatedAt());

        List<AgentTraceNode> nodes = agentTraceNodeMapper.selectList(
                new LambdaQueryWrapper<AgentTraceNode>().eq(AgentTraceNode::getTraceId, id));

        List<Map<String, Object>> nodeMaps = nodes.stream().map(n -> {
            Map<String, Object> nm = new LinkedHashMap<>();
            nm.put("id", n.getId());
            nm.put("nodeId", n.getNodeId());
            nm.put("parentNodeId", n.getParentNodeId());
            nm.put("name", n.getName());
            nm.put("type", n.getType());
            nm.put("status", n.getStatus());
            nm.put("input", n.getInput());
            nm.put("output", n.getOutput());
            nm.put("tokenCount", n.getTokenCount());
            nm.put("modelName", n.getModelName());
            nm.put("errorMessage", n.getErrorMessage());
            nm.put("startedAt", n.getStartedAt());
            nm.put("finishedAt", n.getFinishedAt());
            nm.put("duration", n.getDuration());
            return nm;
        }).collect(Collectors.toList());

        map.put("nodes", nodeMaps);
        return ApiResponse.success(map);
    }
}
