package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.ai.Evaluation;
import com.platform.entity.ai.PromptVersion;
import com.platform.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    // === Prompt Versions ===

    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<PageResult<Map<String, Object>>> listVersions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String stage) {

        Page<PromptVersion> pvPage = promptService.pageVersions(page, pageSize, documentType, stage);

        List<Map<String, Object>> records = pvPage.getRecords().stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("name", v.getName());
            map.put("version", v.getVersion());
            map.put("documentType", v.getDocumentType());
            map.put("stage", v.getStage());
            map.put("content", v.getContent());
            map.put("variables", v.getVariables());
            map.put("author", v.getAuthor());
            map.put("status", v.getStatus());
            map.put("commitMessage", v.getCommitMessage());
            map.put("gitCommitHash", v.getGitCommitHash());
            map.put("createTime", v.getCreatedAt());
            map.put("updateTime", v.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, pvPage.getTotal(), pvPage.getCurrent(), pvPage.getSize()));
    }

    @GetMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Map<String, Object>> versionDetail(@PathVariable Long id) {
        PromptVersion v = promptService.getVersionById(id);
        if (v == null) return ApiResponse.success(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", v.getId());
        map.put("name", v.getName());
        map.put("version", v.getVersion());
        map.put("documentType", v.getDocumentType());
        map.put("stage", v.getStage());
        map.put("content", v.getContent());
        map.put("variables", v.getVariables());
        map.put("author", v.getAuthor());
        map.put("status", v.getStatus());
        map.put("commitMessage", v.getCommitMessage());
        map.put("gitCommitHash", v.getGitCommitHash());
        map.put("createTime", v.getCreatedAt());
        map.put("updateTime", v.getUpdatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping("/versions")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Map<String, Object>> createVersion(@RequestBody Map<String, Object> body) {
        Long id = promptService.createVersion(body);
        return ApiResponse.success(Map.of("id", id));
    }

    @PutMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Void> updateVersion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PromptVersion v = promptService.getVersionById(id);
        if (v == null) return ApiResponse.error(404, "版本不存在");
        promptService.updateVersion(id, body);
        return ApiResponse.success();
    }

    @PostMapping("/versions/{id}/rollback")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Map<String, Object>> rollback(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String author = body != null ? (String) body.get("author") : "system";
        PromptVersion rollbackVersion = promptService.rollback(id, author);
        if (rollbackVersion == null) return ApiResponse.error(404, "版本不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rollbackVersion.getId());
        result.put("version", rollbackVersion.getVersion());
        result.put("status", rollbackVersion.getStatus());
        return ApiResponse.success(result);
    }

    @GetMapping("/versions/diff")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Map<String, Object>> diff(
            @RequestParam Long versionA, @RequestParam Long versionB) {
        return ApiResponse.success(promptService.diff(versionA, versionB));
    }

    // === Evaluations ===

    @GetMapping("/evaluation")
    @PreAuthorize("hasAuthority('prompt:evaluation')")
    public ApiResponse<PageResult<Map<String, Object>>> listEvaluations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<Evaluation> evalPage = promptService.pageEvaluations(page, pageSize);

        List<Map<String, Object>> records = evalPage.getRecords().stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", e.getId());
            map.put("taskName", e.getTaskName());
            map.put("promptVersionId", e.getPromptVersionId());
            map.put("promptVersionName", e.getPromptVersionName());
            map.put("datasetName", e.getDatasetName());
            map.put("datasetSize", e.getDatasetSize());
            map.put("accuracy", e.getAccuracy());
            map.put("precision", e.getPrecisionVal());
            map.put("recall", e.getRecall());
            map.put("f1Score", e.getF1Score());
            map.put("status", e.getStatus());
            map.put("duration", e.getDuration());
            map.put("createTime", e.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, evalPage.getTotal(), evalPage.getCurrent(), evalPage.getSize()));
    }

    @PostMapping("/evaluation/run")
    @PreAuthorize("hasAuthority('prompt:evaluation')")
    public ApiResponse<Map<String, Object>> runEvaluation(@RequestBody Map<String, Object> body) {
        Evaluation e = promptService.runEvaluation(body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", e.getId());
        result.put("status", e.getStatus());
        return ApiResponse.success(result);
    }
}
