package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.log.OperationLog;
import com.platform.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('log:operation')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status) {

        Page<OperationLog> logPage = operationLogService.pageQuery(page, pageSize, operationType, module, status);

        List<Map<String, Object>> records = logPage.getRecords().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("userId", log.getUserId());
            map.put("username", log.getUsername());
            map.put("operationType", log.getOperationType());
            map.put("module", log.getModule());
            map.put("description", log.getDescription());
            map.put("requestParams", log.getRequestParams());
            map.put("responseResult", log.getResponseResult());
            map.put("ip", log.getIp());
            map.put("duration", log.getDuration());
            map.put("status", log.getStatus());
            map.put("createTime", log.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, logPage.getTotal(), logPage.getCurrent(), logPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('log:operation')")
    public ApiResponse<OperationLog> detail(@PathVariable Long id) {
        return ApiResponse.success(operationLogService.getById(id));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('log:operation')")
    public ApiResponse<List<Map<String, Object>>> exportLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String module) {
        // Stub: return recent 100 logs
        Page<OperationLog> logPage = operationLogService.pageQuery(1, 100, operationType, module, null);
        List<Map<String, Object>> records = logPage.getRecords().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("userId", log.getUserId());
            map.put("username", log.getUsername());
            map.put("operationType", log.getOperationType());
            map.put("module", log.getModule());
            map.put("description", log.getDescription());
            map.put("status", log.getStatus());
            map.put("createTime", log.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('log:operation')")
    public ApiResponse<List<Map<String, String>>> types() {
        List<Map<String, String>> list = Arrays.asList(
            Map.of("value", "login", "label", "登录"),
            Map.of("value", "logout", "label", "登出"),
            Map.of("value", "query", "label", "查询"),
            Map.of("value", "create", "label", "创建"),
            Map.of("value", "update", "label", "更新"),
            Map.of("value", "delete", "label", "删除"),
            Map.of("value", "export", "label", "导出"),
            Map.of("value", "grant", "label", "授权"),
            Map.of("value", "other", "label", "其他")
        );
        return ApiResponse.success(list);
    }
}
