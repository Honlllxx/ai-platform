package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.ai.ModelAsset;
import com.platform.entity.ai.ModelBinding;
import com.platform.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    // === Model Assets ===

    @GetMapping
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<PageResult<Map<String, Object>>> listAssets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type) {

        Page<ModelAsset> assetPage = modelService.pageAssets(page, pageSize, type);

        List<Map<String, Object>> records = assetPage.getRecords().stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("version", a.getVersion());
            map.put("type", a.getType());
            map.put("description", a.getDescription());
            map.put("size", a.getSize());
            map.put("filePath", a.getFilePath());
            map.put("framework", a.getFramework());
            map.put("status", a.getStatus());
            map.put("metrics", a.getMetrics());
            map.put("createTime", a.getCreatedAt());
            map.put("updateTime", a.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, assetPage.getTotal(), assetPage.getCurrent(), assetPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Map<String, Object>> assetDetail(@PathVariable Long id) {
        ModelAsset a = modelService.getAssetById(id);
        if (a == null) return ApiResponse.success(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("name", a.getName());
        map.put("version", a.getVersion());
        map.put("type", a.getType());
        map.put("description", a.getDescription());
        map.put("size", a.getSize());
        map.put("filePath", a.getFilePath());
        map.put("framework", a.getFramework());
        map.put("status", a.getStatus());
        map.put("metrics", a.getMetrics());
        map.put("createTime", a.getCreatedAt());
        map.put("updateTime", a.getUpdatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Map<String, Object>> createAsset(@RequestBody Map<String, Object> body) {
        Long id = modelService.createAsset(body);
        return ApiResponse.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> updateAsset(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ModelAsset a = modelService.getAssetById(id);
        if (a == null) return ApiResponse.error(404, "模型不存在");
        modelService.updateAsset(id, body);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> deleteAsset(@PathVariable Long id) {
        modelService.deleteAsset(id);
        return ApiResponse.success();
    }

    // === Model Bindings ===

    @GetMapping("/bindings/list")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<PageResult<Map<String, Object>>> listBindings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<ModelBinding> bindingPage = modelService.pageBindings(page, pageSize);

        List<Map<String, Object>> records = bindingPage.getRecords().stream().map(b -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("bindingName", b.getBindingName());
            map.put("baseModelId", b.getBaseModelId());
            map.put("loraAdapterId", b.getLoraAdapterId());
            map.put("datasetId", b.getDatasetId());
            map.put("status", b.getStatus());
            map.put("createTime", b.getCreatedAt());
            map.put("updateTime", b.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, bindingPage.getTotal(), bindingPage.getCurrent(), bindingPage.getSize()));
    }

    @PostMapping("/bindings")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Map<String, Object>> createBinding(@RequestBody Map<String, Object> body) {
        Long id = modelService.createBinding(body);
        return ApiResponse.success(Map.of("id", id));
    }

    @DeleteMapping("/bindings/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> deleteBinding(@PathVariable Long id) {
        modelService.deleteBinding(id);
        return ApiResponse.success();
    }

    @PostMapping("/bindings/{id}/activate")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> activateBinding(@PathVariable Long id) {
        modelService.activateBinding(id);
        return ApiResponse.success();
    }
}
