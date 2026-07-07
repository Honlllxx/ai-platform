package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.ai.FinetuneTask;
import com.platform.service.FinetuneService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finetune")
@RequiredArgsConstructor
public class FinetuneController {

    private final FinetuneService finetuneService;

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('finetune:list')")
    public ApiResponse<PageResult<Map<String, Object>>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {

        Page<FinetuneTask> taskPage = finetuneService.pageQuery(page, pageSize, status);

        List<Map<String, Object>> records = taskPage.getRecords().stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("taskName", t.getTaskName());
            map.put("baseModel", t.getBaseModel());
            map.put("datasetName", t.getDatasetName());
            map.put("datasetSize", t.getDatasetSize());
            map.put("status", t.getStatus());
            map.put("progress", t.getProgress());
            map.put("epochs", t.getEpochs());
            map.put("currentEpoch", t.getCurrentEpoch());
            map.put("learningRate", t.getLearningRate());
            map.put("loss", t.getLoss());
            map.put("metrics", t.getMetrics());
            map.put("outputModel", t.getOutputModel());
            map.put("errorMessage", t.getErrorMessage());
            map.put("startTime", t.getStartedAt());
            map.put("endTime", t.getFinishedAt());
            map.put("duration", t.getDuration());
            map.put("createTime", t.getCreatedAt());
            map.put("updateTime", t.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, taskPage.getTotal(), taskPage.getCurrent(), taskPage.getSize()));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('finetune:list')")
    public ApiResponse<Map<String, Object>> taskDetail(@PathVariable Long id) {
        FinetuneTask t = finetuneService.getById(id);
        if (t == null) return ApiResponse.success(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("taskName", t.getTaskName());
        map.put("baseModel", t.getBaseModel());
        map.put("datasetName", t.getDatasetName());
        map.put("datasetSize", t.getDatasetSize());
        map.put("status", t.getStatus());
        map.put("progress", t.getProgress());
        map.put("epochs", t.getEpochs());
        map.put("currentEpoch", t.getCurrentEpoch());
        map.put("learningRate", t.getLearningRate());
        map.put("loss", t.getLoss());
        map.put("metrics", t.getMetrics());
        map.put("outputModel", t.getOutputModel());
        map.put("errorMessage", t.getErrorMessage());
        map.put("startTime", t.getStartedAt());
        map.put("endTime", t.getFinishedAt());
        map.put("duration", t.getDuration());
        map.put("createTime", t.getCreatedAt());
        map.put("updateTime", t.getUpdatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('finetune:list')")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody Map<String, Object> body) {
        FinetuneTask t = finetuneService.createTask(body);
        return ApiResponse.success(Map.of("id", t.getId(), "status", t.getStatus()));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('finetune:list')")
    public ApiResponse<Void> cancelTask(@PathVariable Long id) {
        finetuneService.cancelTask(id);
        return ApiResponse.success();
    }
}
