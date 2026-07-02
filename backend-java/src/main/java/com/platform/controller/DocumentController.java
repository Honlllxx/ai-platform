package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.document.Document;
import com.platform.security.JwtTokenProvider;
import com.platform.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_document:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status) {

        Page<Document> docPage = documentService.pageQuery(page, pageSize, fileName, fileType, status);

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
    @PreAuthorize("hasAuthority('ROLE_document:list')")
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
    @PreAuthorize("hasAuthority('ROLE_document:upload')")
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
    @PreAuthorize("hasAuthority('ROLE_document:list')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
