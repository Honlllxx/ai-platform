package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.document.Document;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.pipeline.ReflectionResult;
import com.platform.entity.pipeline.ValidationResult;
import com.platform.service.AiPipelineService;
import com.platform.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;
    private final AiPipelineService aiPipelineService;

    @GetMapping
    @PreAuthorize("hasAuthority('pipeline:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {

        Page<Pipeline> pipelinePage = pipelineService.pageQuery(page, pageSize, status);

        // Batch fetch document names
        Set<Long> docIds = pipelinePage.getRecords().stream()
                .map(Pipeline::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> docNameMap = pipelineService.getDocumentNameMap(docIds);

        List<Map<String, Object>> records = pipelinePage.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("documentId", p.getDocumentId());
            map.put("documentName", docNameMap.getOrDefault(p.getDocumentId(), ""));
            map.put("taskId", p.getTaskId());
            map.put("status", p.getStatus());
            map.put("currentStep", p.getCurrentStep());
            map.put("totalSteps", p.getTotalSteps());
            map.put("completedSteps", p.getCompletedSteps());
            map.put("duration", p.getDuration());
            map.put("error", p.getErrorMessage());
            map.put("startTime", p.getStartedAt());
            map.put("endTime", p.getFinishedAt());
            map.put("createTime", p.getCreatedAt());
            map.put("updateTime", p.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, pipelinePage.getTotal(), pipelinePage.getCurrent(), pipelinePage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pipeline:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Pipeline p = pipelineService.getById(id);
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
        map.put("error", p.getErrorMessage());
        map.put("startTime", p.getStartedAt());
        map.put("endTime", p.getFinishedAt());
        map.put("createTime", p.getCreatedAt());
        map.put("updateTime", p.getUpdatedAt());

        // Document name
        map.put("documentName", pipelineService.getDocumentName(p.getDocumentId()));

        // Extraction results
        List<ExtractionResult> extractions = pipelineService.getExtractions(id);
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
        List<ValidationResult> validations = pipelineService.getValidations(id);
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
        List<ReflectionResult> reflections = pipelineService.getReflections(id);
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

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('pipeline:list')")
    public ApiResponse<Map<String, Object>> run(
            @RequestParam Long documentId,
            @RequestBody(required = false) List<Map<String, String>> metadataFields) {
        Map<String, Object> result = aiPipelineService.runPipeline(documentId, metadataFields);
        return ApiResponse.success(result);
    }
}
