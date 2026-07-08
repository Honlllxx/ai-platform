package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.document.Document;
import com.platform.entity.document.DocumentLayout;
import com.platform.entity.document.DocumentOcr;
import com.platform.entity.document.DocumentTable;
import com.platform.mapper.document.DocumentLayoutMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.document.DocumentOcrMapper;
import com.platform.mapper.document.DocumentTableMapper;
import com.platform.security.JwtTokenProvider;
import com.platform.dto.PipelineTaskMessage;
import com.platform.service.AiPipelineService;
import com.platform.service.DocumentService;
import com.platform.service.RocketMQProducerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final DocumentLayoutMapper documentLayoutMapper;
    private final DocumentOcrMapper documentOcrMapper;
    private final DocumentTableMapper documentTableMapper;
    private final AiPipelineService aiPipelineService;
    private final RocketMQProducerService rocketMQProducerService;

    @GetMapping
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status) {

        Page<Document> docPage = documentService.pageQuery(page, pageSize, keyword, fileType, status);

        List<Map<String, Object>> records = docPage.getRecords().stream().map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("fileName", doc.getFileName());
            map.put("fileType", doc.getFileType());
            map.put("fileSize", doc.getFileSize());
            map.put("fileUrl", doc.getFileUrl());
            map.put("pageCount", doc.getPageCount());
            map.put("uploadUserId", doc.getUploadUserId());
            map.put("status", doc.getStatus());
            map.put("errorMessage", doc.getErrorMessage());
            map.put("createTime", doc.getCreatedAt());
            map.put("updateTime", doc.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, docPage.getTotal(), docPage.getCurrent(), docPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Document doc = documentService.getById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("fileName", doc.getFileName());
        map.put("fileType", doc.getFileType());
        map.put("fileSize", doc.getFileSize());
        map.put("fileUrl", doc.getFileUrl());
        map.put("pageCount", doc.getPageCount());
        map.put("uploadUserId", doc.getUploadUserId());
        map.put("status", doc.getStatus());
        map.put("errorMessage", doc.getErrorMessage());
        map.put("createTime", doc.getCreatedAt());
        map.put("updateTime", doc.getUpdatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('document:upload')")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        Document doc = documentService.upload(file, userId);

        Map<String, Object> map = new HashMap<>();
        map.put("id", doc.getId());
        map.put("fileName", doc.getFileName());
        map.put("fileUrl", doc.getFileUrl());
        map.put("fileSize", doc.getFileSize());
        map.put("status", doc.getStatus());
        return ApiResponse.success(map);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success();
    }

    // === Sub-resource endpoints ===

    @GetMapping("/{id}/layout")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> layout(@PathVariable Long id) {
        DocumentLayout layout = documentLayoutMapper.selectOne(
                new LambdaQueryWrapper<DocumentLayout>().eq(DocumentLayout::getDocumentId, id)
                        .orderByDesc(DocumentLayout::getCreatedAt).last("LIMIT 1"));

        if (layout == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", layout.getId());
        map.put("documentId", layout.getDocumentId());
        map.put("imageUrl", layout.getImageUrl());
        map.put("regions", layout.getRegions());
        map.put("confidence", layout.getConfidence());
        map.put("createTime", layout.getCreatedAt());
        return ApiResponse.success(map);
    }

    @GetMapping("/{id}/ocr")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> ocr(@PathVariable Long id) {
        DocumentOcr ocr = documentOcrMapper.selectOne(
                new LambdaQueryWrapper<DocumentOcr>().eq(DocumentOcr::getDocumentId, id)
                        .orderByDesc(DocumentOcr::getCreatedAt).last("LIMIT 1"));

        if (ocr == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ocr.getId());
        map.put("documentId", ocr.getDocumentId());
        map.put("totalChars", ocr.getTotalChars());
        map.put("avgConfidence", ocr.getAvgConfidence());
        map.put("language", ocr.getLanguage());
        map.put("entries", ocr.getEntries());
        map.put("createTime", ocr.getCreatedAt());
        return ApiResponse.success(map);
    }

    @GetMapping("/{id}/table")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> table(@PathVariable Long id) {
        DocumentTable table = documentTableMapper.selectOne(
                new LambdaQueryWrapper<DocumentTable>().eq(DocumentTable::getDocumentId, id)
                        .orderByDesc(DocumentTable::getCreatedAt).last("LIMIT 1"));

        if (table == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", table.getId());
        map.put("documentId", table.getDocumentId());
        map.put("confidence", table.getConfidence());
        map.put("originalTable", table.getOriginalTable());
        map.put("extractedTable", table.getExtractedTable());
        map.put("createTime", table.getCreatedAt());
        return ApiResponse.success(map);
    }

    @PostMapping("/{id}/preprocess")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> preprocess(@PathVariable Long id) {
        Map<String, Object> result = aiPipelineService.preprocessDocument(id);
        return ApiResponse.success(result);
    }

    /**
     * Submit document for async AI processing via RocketMQ.
     * Returns immediately with a taskId — processing happens in the background.
     * Poll GET /api/pipeline for status updates.
     */
    @PostMapping("/{id}/pipeline")
    @PreAuthorize("hasAuthority('document:list')")
    public ApiResponse<Map<String, Object>> submitPipeline(
            @PathVariable Long id,
            @RequestBody(required = false) List<Map<String, String>> metadataFields,
            HttpServletRequest request) {

        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return ApiResponse.error(404, "文档不存在");
        }

        // Update document status
        doc.setStatus("queued");
        documentMapper.updateById(doc);

        // Build task message
        String taskId = UUID.randomUUID().toString();
        Long userId = getCurrentUserId(request);

        PipelineTaskMessage task = new PipelineTaskMessage();
        task.setDocumentId(id);
        task.setTaskId(taskId);
        task.setFileName(doc.getFileName());
        task.setFileUrl(doc.getFileUrl());
        task.setFileType(doc.getFileType());
        task.setMetadataFields(metadataFields);
        task.setUserId(userId);
        task.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Send to RocketMQ
        rocketMQProducerService.submitPipelineTask(task);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", id);
        result.put("taskId", taskId);
        result.put("status", "queued");
        result.put("message", "文档已提交异步处理，可通过 Pipeline API 查询进度");
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return jwtTokenProvider.getUserIdFromToken(token);
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
