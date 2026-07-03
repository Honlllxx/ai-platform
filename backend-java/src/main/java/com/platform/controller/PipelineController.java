package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.pipeline.ReflectionResult;
import com.platform.entity.pipeline.ValidationResult;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import com.platform.mapper.pipeline.ReflectionResultMapper;
import com.platform.mapper.pipeline.ValidationResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineMapper pipelineMapper;
    private final ExtractionResultMapper extractionResultMapper;
    private final ValidationResultMapper validationResultMapper;
    private final ReflectionResultMapper reflectionResultMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_pipeline:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {

        LambdaQueryWrapper<Pipeline> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Pipeline::getStatus, status);
        }
        wrapper.orderByDesc(Pipeline::getCreatedAt);

        Page<Pipeline> pipelinePage = pipelineMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> records = pipelinePage.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("documentId", p.getDocumentId());
            map.put("taskId", p.getTaskId());
            map.put("status", p.getStatus());
            map.put("currentStep", p.getCurrentStep());
            map.put("totalSteps", p.getTotalSteps());
            map.put("completedSteps", p.getCompletedSteps());
            map.put("duration", p.getDuration());
            map.put("errorMessage", p.getErrorMessage());
            map.put("startedAt", p.getStartedAt());
            map.put("finishedAt", p.getFinishedAt());
            map.put("createTime", p.getCreatedAt());
            map.put("updateTime", p.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, pipelinePage.getTotal(), pipelinePage.getCurrent(), pipelinePage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_pipeline:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Pipeline p = pipelineMapper.selectById(id);
        if (p == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("documentId", p.getDocumentId());
        map.put("taskId", p.getTaskId());
        map.put("status", p.getStatus());
        map.put("currentStep", p.getCurrentStep());
        map.put("totalSteps", p.getTotalSteps());
        map.put("completedSteps", p.getCompletedSteps());
        map.put("duration", p.getDuration());
        map.put("stateGraph", p.getStateGraph());
        map.put("errorMessage", p.getErrorMessage());
        map.put("startedAt", p.getStartedAt());
        map.put("finishedAt", p.getFinishedAt());
        map.put("createTime", p.getCreatedAt());
        map.put("updateTime", p.getUpdatedAt());

        // Extraction results
        List<ExtractionResult> extractions = extractionResultMapper.selectList(
                new LambdaQueryWrapper<ExtractionResult>().eq(ExtractionResult::getPipelineId, id));
        map.put("extractionResults", extractions.stream().map(e -> {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("id", e.getId());
            em.put("fieldName", e.getFieldName());
            em.put("fieldCode", e.getFieldCode());
            em.put("extractedValue", e.getExtractedValue());
            em.put("confidence", e.getConfidence());
            em.put("status", e.getStatus());
            em.put("pageRef", e.getPageRef());
            return em;
        }).collect(Collectors.toList()));

        // Validation results
        List<ValidationResult> validations = validationResultMapper.selectList(
                new LambdaQueryWrapper<ValidationResult>().eq(ValidationResult::getPipelineId, id));
        map.put("validationResults", validations.stream().map(v -> {
            Map<String, Object> vm = new LinkedHashMap<>();
            vm.put("id", v.getId());
            vm.put("rule", v.getRule());
            vm.put("fieldName", v.getFieldName());
            vm.put("severity", v.getSeverity());
            vm.put("message", v.getMessage());
            vm.put("suggestion", v.getSuggestion());
            return vm;
        }).collect(Collectors.toList()));

        // Reflection results
        List<ReflectionResult> reflections = reflectionResultMapper.selectList(
                new LambdaQueryWrapper<ReflectionResult>().eq(ReflectionResult::getPipelineId, id));
        map.put("reflectionResults", reflections.stream().map(r -> {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("fieldName", r.getFieldName());
            rm.put("beforeValue", r.getBeforeValue());
            rm.put("afterValue", r.getAfterValue());
            rm.put("reason", r.getReason());
            return rm;
        }).collect(Collectors.toList()));

        return ApiResponse.success(map);
    }
}
