package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.ai.Evaluation;
import com.platform.entity.ai.PromptVersion;
import com.platform.mapper.ai.EvaluationMapper;
import com.platform.mapper.ai.PromptVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final PromptVersionMapper promptVersionMapper;
    private final EvaluationMapper evaluationMapper;

    // === Prompt Versions ===

    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<PageResult<Map<String, Object>>> listVersions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String stage) {

        LambdaQueryWrapper<PromptVersion> wrapper = new LambdaQueryWrapper<>();
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(PromptVersion::getDocumentType, documentType);
        }
        if (stage != null && !stage.isBlank()) {
            wrapper.eq(PromptVersion::getStage, stage);
        }
        wrapper.orderByDesc(PromptVersion::getCreatedAt);

        Page<PromptVersion> pvPage = promptVersionMapper.selectPage(new Page<>(page, pageSize), wrapper);

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
        PromptVersion v = promptVersionMapper.selectById(id);
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
        PromptVersion v = new PromptVersion();
        v.setName((String) body.get("name"));
        v.setVersion((String) body.get("version"));
        v.setDocumentType((String) body.get("documentType"));
        v.setStage((String) body.get("stage"));
        v.setContent((String) body.get("content"));
        v.setVariables((String) body.get("variables"));
        v.setAuthor((String) body.get("author"));
        v.setStatus((String) body.getOrDefault("status", "draft"));
        v.setCommitMessage((String) body.get("commitMessage"));
        promptVersionMapper.insert(v);
        return ApiResponse.success(Map.of("id", v.getId()));
    }

    @PutMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Void> updateVersion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PromptVersion v = promptVersionMapper.selectById(id);
        if (v == null) return ApiResponse.error(404, "版本不存在");
        if (body.containsKey("name")) v.setName((String) body.get("name"));
        if (body.containsKey("content")) v.setContent((String) body.get("content"));
        if (body.containsKey("variables")) v.setVariables((String) body.get("variables"));
        if (body.containsKey("status")) v.setStatus((String) body.get("status"));
        if (body.containsKey("commitMessage")) v.setCommitMessage((String) body.get("commitMessage"));
        promptVersionMapper.updateById(v);
        return ApiResponse.success();
    }

    @PostMapping("/versions/{id}/rollback")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Void> rollback(@PathVariable Long id) {
        // Stub: mark previous version as active
        PromptVersion v = promptVersionMapper.selectById(id);
        if (v != null) {
            v.setStatus("active");
            promptVersionMapper.updateById(v);
        }
        return ApiResponse.success();
    }

    @GetMapping("/versions/diff")
    @PreAuthorize("hasAuthority('prompt:version')")
    public ApiResponse<Map<String, Object>> diff(
            @RequestParam Long versionA, @RequestParam Long versionB) {
        PromptVersion a = promptVersionMapper.selectById(versionA);
        PromptVersion b = promptVersionMapper.selectById(versionB);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionA", a != null ? a.getContent() : "");
        result.put("versionB", b != null ? b.getContent() : "");
        result.put("changes", List.of()); // stub: no diff engine
        return ApiResponse.success(result);
    }

    // === Evaluations ===

    @GetMapping("/evaluation")
    @PreAuthorize("hasAuthority('prompt:evaluation')")
    public ApiResponse<PageResult<Map<String, Object>>> listEvaluations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Evaluation::getCreatedAt);
        Page<Evaluation> evalPage = evaluationMapper.selectPage(new Page<>(page, pageSize), wrapper);

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
        // Stub: create an evaluation record
        Evaluation e = new Evaluation();
        e.setTaskName((String) body.getOrDefault("taskName", "auto-eval"));
        e.setPromptVersionId(body.get("promptVersionId") != null
                ? ((Number) body.get("promptVersionId")).longValue() : null);
        e.setDatasetName((String) body.get("datasetName"));
        e.setStatus("running");
        e.setAccuracy(0.0);
        e.setF1Score(0.0);
        evaluationMapper.insert(e);

        // Immediately complete as stub
        e.setStatus("completed");
        e.setAccuracy(85.5);
        e.setF1Score(0.83);
        evaluationMapper.updateById(e);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", e.getId());
        result.put("status", e.getStatus());
        return ApiResponse.success(result);
    }
}
