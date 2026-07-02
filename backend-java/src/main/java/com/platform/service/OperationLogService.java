package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.log.OperationLog;
import com.platform.mapper.log.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Async
    public void save(OperationLog log) {
        operationLogMapper.insert(log);
    }

    public Page<OperationLog> pageQuery(int page, int pageSize, String operationType, String module, String status) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (operationType != null && !operationType.isBlank()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return operationLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public OperationLog getById(Long id) {
        return operationLogMapper.selectById(id);
    }
}
