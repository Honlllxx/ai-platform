package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.log.AgentTrace;
import com.platform.entity.log.AgentTraceNode;
import com.platform.mapper.log.AgentTraceMapper;
import com.platform.mapper.log.AgentTraceNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentTraceService {

    private final AgentTraceMapper agentTraceMapper;
    private final AgentTraceNodeMapper agentTraceNodeMapper;

    public Page<AgentTrace> pageQuery(int page, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<AgentTrace> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(AgentTrace::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AgentTrace::getTraceId, keyword)
                    .or().like(AgentTrace::getUsername, keyword));
        }
        wrapper.orderByDesc(AgentTrace::getCreatedAt);
        return agentTraceMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public AgentTrace getById(Long id) {
        return agentTraceMapper.selectById(id);
    }

    public List<AgentTraceNode> getNodesByTraceId(Long traceId) {
        return agentTraceNodeMapper.selectList(
                new LambdaQueryWrapper<AgentTraceNode>().eq(AgentTraceNode::getTraceId, traceId));
    }
}
