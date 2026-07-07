package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.ai.ModelAsset;
import com.platform.entity.ai.ModelBinding;
import com.platform.mapper.ai.ModelAssetMapper;
import com.platform.mapper.ai.ModelBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelAssetMapper modelAssetMapper;
    private final ModelBindingMapper modelBindingMapper;

    // === Model Assets ===

    public Page<ModelAsset> pageAssets(int page, int pageSize, String type) {
        LambdaQueryWrapper<ModelAsset> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(ModelAsset::getType, type);
        }
        wrapper.orderByDesc(ModelAsset::getCreatedAt);
        return modelAssetMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public ModelAsset getAssetById(Long id) {
        return modelAssetMapper.selectById(id);
    }

    public Long createAsset(Map<String, Object> body) {
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
        return a.getId();
    }

    public void updateAsset(Long id, Map<String, Object> body) {
        ModelAsset a = modelAssetMapper.selectById(id);
        if (a == null) return;
        if (body.containsKey("name")) a.setName((String) body.get("name"));
        if (body.containsKey("description")) a.setDescription((String) body.get("description"));
        if (body.containsKey("status")) a.setStatus((String) body.get("status"));
        if (body.containsKey("metrics")) a.setMetrics((String) body.get("metrics"));
        modelAssetMapper.updateById(a);
    }

    public void deleteAsset(Long id) {
        modelAssetMapper.deleteById(id);
    }

    // === Model Bindings ===

    public Page<ModelBinding> pageBindings(int page, int pageSize) {
        LambdaQueryWrapper<ModelBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ModelBinding::getCreatedAt);
        return modelBindingMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Long createBinding(Map<String, Object> body) {
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
        return b.getId();
    }

    public void deleteBinding(Long id) {
        modelBindingMapper.deleteById(id);
    }

    public void activateBinding(Long id) {
        ModelBinding b = modelBindingMapper.selectById(id);
        if (b != null) {
            b.setStatus("active");
            modelBindingMapper.updateById(b);
        }
    }
}
