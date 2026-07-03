package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.MetadataField;
import com.platform.entity.common.ExportHistory;
import com.platform.entity.common.WebhookConfig;
import com.platform.mapper.common.ExportHistoryMapper;
import com.platform.mapper.common.WebhookConfigMapper;
import com.platform.service.MetadataFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataFieldService metadataFieldService;
    private final WebhookConfigMapper webhookConfigMapper;
    private final ExportHistoryMapper exportHistoryMapper;

    @GetMapping("/fields")
    @PreAuthorize("hasAuthority('metadata:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        Page<MetadataField> fieldPage = metadataFieldService.pageQuery(page, pageSize, groupCode, keyword, status);

        List<Map<String, Object>> records = fieldPage.getRecords().stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("name", f.getName());
            map.put("code", f.getCode());
            map.put("type", f.getType());
            map.put("length", f.getLength());
            map.put("enumValues", f.getEnumValues());
            map.put("validationRule", f.getValidationRule());
            map.put("groupCode", f.getGroupCode());
            map.put("groupName", f.getGroupName());
            map.put("required", f.getRequired());
            map.put("description", f.getDescription());
            map.put("sort", f.getSort());
            map.put("status", f.getStatus());
            map.put("createTime", f.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, fieldPage.getTotal(), fieldPage.getCurrent(), fieldPage.getSize()));
    }

    @GetMapping("/fields/{id}")
    @PreAuthorize("hasAuthority('metadata:list')")
    public ApiResponse<MetadataField> detail(@PathVariable Long id) {
        return ApiResponse.success(metadataFieldService.getById(id));
    }

    @PostMapping("/fields")
    @PreAuthorize("hasAuthority('metadata:list')")
    public ApiResponse<MetadataField> create(@RequestBody MetadataField field) {
        return ApiResponse.success(metadataFieldService.create(field));
    }

    @PutMapping("/fields/{id}")
    @PreAuthorize("hasAuthority('metadata:update')")
    public ApiResponse<MetadataField> update(@PathVariable Long id, @RequestBody MetadataField field) {
        return ApiResponse.success(metadataFieldService.update(id, field));
    }

    @DeleteMapping("/fields/{id}")
    @PreAuthorize("hasAuthority('metadata:update')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        metadataFieldService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('metadata:list')")
    public ApiResponse<List<String>> groupsAlias() {
        return ApiResponse.success(metadataFieldService.getGroups());
    }

    @GetMapping("/fields/groups")
    @PreAuthorize("hasAuthority('metadata:list')")
    public ApiResponse<List<String>> groups() {
        return ApiResponse.success(metadataFieldService.getGroups());
    }

    @GetMapping("/fields/by-group/{groupCode}")
    @PreAuthorize("hasAuthority('metadata:export')")
    public ApiResponse<List<MetadataField>> byGroup(@PathVariable String groupCode) {
        return ApiResponse.success(metadataFieldService.getByGroupCode(groupCode));
    }

    // ── Export ──────────────────────────────────────────

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('metadata:export')")
    public ApiResponse<Map<String, Object>> exportData(@RequestBody Map<String, Object> body) {
        String format = (String) body.getOrDefault("format", "JSON");
        @SuppressWarnings("unchecked")
        List<Integer> docIds = (List<Integer>) body.getOrDefault("documentIds", Collections.emptyList());

        ExportHistory history = new ExportHistory();
        history.setFormat(format);
        history.setRecordCount(docIds.size());
        history.setCreatedAt(LocalDateTime.now());
        exportHistoryMapper.insert(history);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", history.getId());
        result.put("recordCount", docIds.size());
        result.put("format", format);
        return ApiResponse.success(result);
    }

    @GetMapping("/export/history")
    @PreAuthorize("hasAuthority('metadata:export')")
    public ApiResponse<List<Map<String, Object>>> exportHistory() {
        List<ExportHistory> list = exportHistoryMapper.selectList(
                new LambdaQueryWrapper<ExportHistory>().orderByDesc(ExportHistory::getCreatedAt));

        List<Map<String, Object>> records = list.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("format", h.getFormat());
            m.put("fileUrl", h.getFileUrl());
            m.put("recordCount", h.getRecordCount());
            m.put("createTime", h.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.success(records);
    }

    // ── Webhook Config ──────────────────────────────────

    @GetMapping("/export/webhook")
    @PreAuthorize("hasAuthority('metadata:export')")
    public ApiResponse<Map<String, Object>> getWebhook() {
        WebhookConfig config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<WebhookConfig>().last("LIMIT 1"));
        if (config == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("url", "");
            empty.put("secret", "");
            empty.put("enabled", false);
            empty.put("events", Collections.emptyList());
            return ApiResponse.success(empty);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", config.getId());
        map.put("url", config.getUrl());
        map.put("secret", config.getSecret());
        map.put("enabled", config.getEnabled());
        map.put("events", config.getEvents());
        return ApiResponse.success(map);
    }

    @PutMapping("/export/webhook")
    @PreAuthorize("hasAuthority('metadata:export')")
    public ApiResponse<Map<String, Object>> updateWebhook(@RequestBody Map<String, Object> body) {
        WebhookConfig config = webhookConfigMapper.selectOne(
                new LambdaQueryWrapper<WebhookConfig>().last("LIMIT 1"));
        if (config == null) {
            config = new WebhookConfig();
        }
        if (body.containsKey("url")) config.setUrl((String) body.get("url"));
        if (body.containsKey("secret")) config.setSecret((String) body.get("secret"));
        if (body.containsKey("enabled")) config.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("events")) config.setEvents((String) body.get("events")); // JSON string

        if (config.getId() == null) {
            webhookConfigMapper.insert(config);
        } else {
            webhookConfigMapper.updateById(config);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", config.getUrl());
        result.put("secret", config.getSecret());
        result.put("enabled", config.getEnabled());
        result.put("events", config.getEvents());
        return ApiResponse.success(result);
    }
}
