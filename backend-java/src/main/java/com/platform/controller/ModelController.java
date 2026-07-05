package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.ai.ModelAsset;
import com.platform.entity.ai.ModelBinding;
import com.platform.mapper.ai.ModelAssetMapper;
import com.platform.mapper.ai.ModelBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelAssetMapper modelAssetMapper;
    private final ModelBindingMapper modelBindingMapper;

    // === Model Assets ===

    @GetMapping
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<PageResult<Map<String, Object>>> listAssets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type) {

        LambdaQueryWrapper<ModelAsset> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(ModelAsset::getType, type);
        }
        wrapper.orderByDesc(ModelAsset::getCreatedAt);
        Page<ModelAsset> assetPage = modelAssetMapper.selectPage(new Page<>(page, pageSize), wrapper);

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
        ModelAsset a = modelAssetMapper.selectById(id);
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
        ModelAsset a = new ModelAsset();
        a.setName((String) body.get("name"));
        a.setVersion((String) body.get("version"));
        a.setType((String) body.get("type"));
        a.setDescription((String) body.get("description"));
        a.setSize((String) body.get("size"));
        a.setFilePath((String) body.get("filePath"));
        a.setFramework((String) body.get("framework"));
        a.setStatus((String) body.getOrDefault("status", "active"));
        a.setMetrics((String) body.get("metrics"));
        modelAssetMapper.insert(a);
        return ApiResponse.success(Map.of("id", a.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> updateAsset(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ModelAsset a = modelAssetMapper.selectById(id);
        if (a == null) return ApiResponse.error(404, "模型不存在");
        if (body.containsKey("name")) a.setName((String) body.get("name"));
        if (body.containsKey("description")) a.setDescription((String) body.get("description"));
        if (body.containsKey("status")) a.setStatus((String) body.get("status"));
        if (body.containsKey("metrics")) a.setMetrics((String) body.get("metrics"));
        modelAssetMapper.updateById(a);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> deleteAsset(@PathVariable Long id) {
        modelAssetMapper.deleteById(id);
        return ApiResponse.success();
    }

    // === Model Bindings ===

    @GetMapping("/bindings/list")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<PageResult<Map<String, Object>>> listBindings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<ModelBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ModelBinding::getCreatedAt);
        Page<ModelBinding> bindingPage = modelBindingMapper.selectPage(new Page<>(page, pageSize), wrapper);

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
        ModelBinding b = new ModelBinding();
        b.setBindingName((String) body.get("bindingName"));
        b.setBaseModelId(body.get("baseModelId") != null
                ? ((Number) body.get("baseModelId")).longValue() : null);
        b.setLoraAdapterId(body.get("loraAdapterId") != null
                ? ((Number) body.get("loraAdapterId")).longValue() : null);
        b.setDatasetId(body.get("datasetId") != null
                ? ((Number) body.get("datasetId")).longValue() : null);
        b.setStatus((String) body.getOrDefault("status", "active"));
        modelBindingMapper.insert(b);
        return ApiResponse.success(Map.of("id", b.getId()));
    }

    @DeleteMapping("/bindings/{id}")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> deleteBinding(@PathVariable Long id) {
        modelBindingMapper.deleteById(id);
        return ApiResponse.success();
    }

    @PostMapping("/bindings/{id}/activate")
    @PreAuthorize("hasAuthority('model:list')")
    public ApiResponse<Void> activateBinding(@PathVariable Long id) {
        ModelBinding b = modelBindingMapper.selectById(id);
        if (b != null) {
            b.setStatus("active");
            modelBindingMapper.updateById(b);
        }
        return ApiResponse.success();
    }
}
