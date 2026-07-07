package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.common.ExportHistory;
import com.platform.entity.log.OperationLog;
import com.platform.mapper.common.ExportHistoryMapper;
import com.platform.security.JwtTokenProvider;
import com.platform.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final ExportHistoryMapper exportHistoryMapper;
    private final JwtTokenProvider jwtTokenProvider;

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
    public void exportLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String module,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        List<OperationLog> logs = operationLogService.exportQuery(operationType, module);

        // Generate CSV
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "operation_logs_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());

        // Write BOM for Excel UTF-8 compatibility
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));

        // CSV Header
        writer.println("ID,用户ID,用户名,操作类型,模块,描述,请求参数,响应结果,IP地址,耗时(ms),状态,操作时间");

        // CSV Rows
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (OperationLog log : logs) {
            writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    escapeCsv(log.getId()),
                    escapeCsv(log.getUserId()),
                    escapeCsv(log.getUsername()),
                    escapeCsv(log.getOperationType()),
                    escapeCsv(log.getModule()),
                    escapeCsv(log.getDescription()),
                    escapeCsv(log.getRequestParams()),
                    escapeCsv(log.getResponseResult()),
                    escapeCsv(log.getIp()),
                    escapeCsv(log.getDuration()),
                    escapeCsv(log.getStatus()),
                    escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "")));
        }
        writer.flush();

        // Save export history
        Long operatorId = getCurrentUserId(request);
        ExportHistory history = new ExportHistory();
        history.setFormat("csv");
        history.setFileUrl(filename);
        history.setRecordCount(logs.size());
        history.setOperatorId(operatorId);
        exportHistoryMapper.insert(history);
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

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // Escape double quotes and wrap in quotes if needed
        if (s.contains("\"") || s.contains(",") || s.contains("\n")) {
            return s.replace("\"", "\"\"");
        }
        return s;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
        }
        return null;
    }
}
