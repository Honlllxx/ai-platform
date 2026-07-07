package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.dto.ApiResponse;
import com.platform.entity.common.SystemSetting;
import com.platform.mapper.common.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemSettingMapper systemSettingMapper;

    /**
     * Get all settings grouped by category.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('settings:view')")
    public ApiResponse<Map<String, Object>> getAll() {
        List<SystemSetting> all = systemSettingMapper.selectList(null);

        Map<String, Object> result = new LinkedHashMap<>();

        // Group by category
        Map<String, List<SystemSetting>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategory() != null ? s.getCategory() : "general"));

        for (Map.Entry<String, List<SystemSetting>> entry : grouped.entrySet()) {
            Map<String, Object> categorySettings = new LinkedHashMap<>();
            for (SystemSetting setting : entry.getValue()) {
                categorySettings.put(setting.getSettingKey(), parseValue(setting.getSettingValue()));
            }
            result.put(entry.getKey(), categorySettings);
        }

        return ApiResponse.success(result);
    }

    /**
     * Get settings for a specific category.
     */
    @GetMapping("/{category}")
    @PreAuthorize("hasAuthority('settings:view')")
    public ApiResponse<Map<String, Object>> getByCategory(@PathVariable String category) {
        List<SystemSetting> settings = systemSettingMapper.selectList(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getCategory, category));

        Map<String, Object> result = new LinkedHashMap<>();
        for (SystemSetting setting : settings) {
            result.put(setting.getSettingKey(), parseValue(setting.getSettingValue()));
        }

        return ApiResponse.success(result);
    }

    /**
     * Batch update settings for a category.
     * Accepts a flat map of key-value pairs.
     */
    @PutMapping("/{category}")
    @PreAuthorize("hasAuthority('settings:view')")
    public ApiResponse<Void> updateCategory(
            @PathVariable String category,
            @RequestBody Map<String, Object> body) {

        for (Map.Entry<String, Object> entry : body.entrySet()) {
            upsertSetting(category, entry.getKey(), entry.getValue());
        }
        return ApiResponse.success();
    }

    private void upsertSetting(String category, String key, Object value) {
        // Find existing
        List<SystemSetting> existing = systemSettingMapper.selectList(
                new LambdaQueryWrapper<SystemSetting>()
                        .eq(SystemSetting::getCategory, category)
                        .eq(SystemSetting::getSettingKey, key));

        String stringValue = value != null ? String.valueOf(value) : "";

        if (!existing.isEmpty()) {
            SystemSetting s = existing.get(0);
            s.setSettingValue(stringValue);
            systemSettingMapper.updateById(s);
        } else {
            SystemSetting s = new SystemSetting();
            s.setCategory(category);
            s.setSettingKey(key);
            s.setSettingValue(stringValue);
            systemSettingMapper.insert(s);
        }
    }

    /**
     * Parse string value to appropriate type (boolean, number, or string).
     */
    private Object parseValue(String value) {
        if (value == null) return "";
        // Boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        // Number (integer)
        try {
            if (value.matches("^-?\\d+$")) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {}
        // Number (decimal)
        try {
            if (value.matches("^-?\\d+\\.\\d+$")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException ignored) {}
        // String
        return value;
    }
}
