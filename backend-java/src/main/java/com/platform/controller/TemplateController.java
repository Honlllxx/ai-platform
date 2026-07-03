package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.Template;
import com.platform.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('template:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        Page<Template> templatePage = templateService.pageQuery(page, pageSize, documentType, keyword, status);

        List<Map<String, Object>> records = templatePage.getRecords().stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("documentType", t.getDocumentType());
            map.put("description", t.getDescription());
            map.put("regions", templateService.parseRegions(t.getRegions()));
            map.put("status", t.getStatus());
            map.put("version", t.getVersion());
            map.put("createTime", t.getCreatedAt());
            map.put("updateTime", t.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, templatePage.getTotal(), templatePage.getCurrent(), templatePage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('template:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Template t = templateService.getById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("documentType", t.getDocumentType());
        map.put("description", t.getDescription());
        map.put("regions", templateService.parseRegions(t.getRegions()));
        map.put("status", t.getStatus());
        map.put("version", t.getVersion());
        map.put("createTime", t.getCreatedAt());
        map.put("updateTime", t.getUpdatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('template:list')")
    public ApiResponse<Template> create(@RequestBody Template template) {
        return ApiResponse.success(templateService.create(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('template:list')")
    public ApiResponse<Template> update(@PathVariable Long id, @RequestBody Template template) {
        return ApiResponse.success(templateService.update(id, template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('template:list')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.success();
    }
}
