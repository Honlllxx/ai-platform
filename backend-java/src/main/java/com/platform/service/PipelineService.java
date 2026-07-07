package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.document.Document;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.pipeline.ReflectionResult;
import com.platform.entity.pipeline.ValidationResult;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import com.platform.mapper.pipeline.ReflectionResultMapper;
import com.platform.mapper.pipeline.ValidationResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineMapper pipelineMapper;
    private final ExtractionResultMapper extractionResultMapper;
    private final ValidationResultMapper validationResultMapper;
    private final ReflectionResultMapper reflectionResultMapper;
    private final DocumentMapper documentMapper;

    public Page<Pipeline> pageQuery(int page, int pageSize, String status) {
        LambdaQueryWrapper<Pipeline> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Pipeline::getStatus, status);
        }
        wrapper.orderByDesc(Pipeline::getCreatedAt);
        return pipelineMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Pipeline getById(Long id) {
        return pipelineMapper.selectById(id);
    }

    public Map<Long, String> getDocumentNameMap(Set<Long> docIds) {
        if (docIds.isEmpty()) return Map.of();
        List<Document> docs = documentMapper.selectBatchIds(docIds);
        return docs.stream().collect(Collectors.toMap(Document::getId, Document::getFileName));
    }

    public String getDocumentName(Long documentId) {
        if (documentId == null) return "";
        Document doc = documentMapper.selectById(documentId);
        return doc != null ? doc.getFileName() : "";
    }

    public List<ExtractionResult> getExtractions(Long pipelineId) {
        return extractionResultMapper.selectList(
                new LambdaQueryWrapper<ExtractionResult>().eq(ExtractionResult::getPipelineId, pipelineId));
    }

    public List<ValidationResult> getValidations(Long pipelineId) {
        return validationResultMapper.selectList(
                new LambdaQueryWrapper<ValidationResult>().eq(ValidationResult::getPipelineId, pipelineId));
    }

    public List<ReflectionResult> getReflections(Long pipelineId) {
        return reflectionResultMapper.selectList(
                new LambdaQueryWrapper<ReflectionResult>().eq(ReflectionResult::getPipelineId, pipelineId));
    }
}
