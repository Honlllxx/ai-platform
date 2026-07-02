package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.MetadataField;
import com.platform.service.MetadataFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataFieldService metadataFieldService;

    @GetMapping("/fields")
    @PreAuthorize("hasAuthority('ROLE_metadata:list')")
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
    @PreAuthorize("hasAuthority('ROLE_metadata:list')")
    public ApiResponse<MetadataField> detail(@PathVariable Long id) {
        return ApiResponse.success(metadataFieldService.getById(id));
    }

    @PostMapping("/fields")
    @PreAuthorize("hasAuthority('ROLE_metadata:list')")
    public ApiResponse<MetadataField> create(@RequestBody MetadataField field) {
        return ApiResponse.success(metadataFieldService.create(field));
    }

    @PutMapping("/fields/{id}")
    @PreAuthorize("hasAuthority('ROLE_metadata:update')")
    public ApiResponse<MetadataField> update(@PathVariable Long id, @RequestBody MetadataField field) {
        return ApiResponse.success(metadataFieldService.update(id, field));
    }

    @DeleteMapping("/fields/{id}")
    @PreAuthorize("hasAuthority('ROLE_metadata:update')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        metadataFieldService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/fields/groups")
    @PreAuthorize("hasAuthority('ROLE_metadata:list')")
    public ApiResponse<List<String>> groups() {
        return ApiResponse.success(metadataFieldService.getGroups());
    }

    @GetMapping("/fields/by-group/{groupCode}")
    @PreAuthorize("hasAuthority('ROLE_metadata:export')")
    public ApiResponse<List<MetadataField>> byGroup(@PathVariable String groupCode) {
        return ApiResponse.success(metadataFieldService.getByGroupCode(groupCode));
    }
}
