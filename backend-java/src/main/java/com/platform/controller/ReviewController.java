package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.Review;
import com.platform.entity.business.ReviewField;
import com.platform.entity.business.ReviewHistory;
import com.platform.security.JwtTokenProvider;
import com.platform.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @PreAuthorize("hasAuthority('review:create')")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Review r = reviewService.createReview(body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("status", r.getStatus());
        return ApiResponse.success(result);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType) {

        Page<Review> reviewPage = reviewService.pageQuery(page, pageSize, status, documentType);

        // Batch fetch document names and reviewer names
        Set<Long> docIds = reviewPage.getRecords().stream()
                .map(Review::getDocumentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> reviewerIds = reviewPage.getRecords().stream()
                .map(Review::getReviewerId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> docNameMap = reviewService.getDocumentNameMap(docIds);
        Map<Long, String> userNameMap = reviewService.getUserNameMap(reviewerIds);

        List<Map<String, Object>> records = reviewPage.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("documentId", r.getDocumentId());
            map.put("documentName", docNameMap.getOrDefault(r.getDocumentId(), ""));
            map.put("pipelineId", r.getPipelineId());
            map.put("documentType", r.getDocumentType());
            map.put("status", r.getStatus());
            map.put("totalFields", r.getTotalFields());
            map.put("reviewedFields", r.getReviewedFields());
            map.put("approvedFields", r.getApprovedFields());
            map.put("rejectedFields", r.getRejectedFields());
            map.put("reviewerId", r.getReviewerId());
            map.put("reviewer", userNameMap.getOrDefault(r.getReviewerId(), ""));
            map.put("reviewedAt", r.getReviewedAt());
            map.put("createTime", r.getCreatedAt());
            map.put("updateTime", r.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, reviewPage.getTotal(), reviewPage.getCurrent(), reviewPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Review r = reviewService.getById(id);
        if (r == null) {
            return ApiResponse.success(null);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("documentId", r.getDocumentId());
        map.put("pipelineId", r.getPipelineId());
        map.put("documentType", r.getDocumentType());
        map.put("status", r.getStatus());
        map.put("totalFields", r.getTotalFields());
        map.put("reviewedFields", r.getReviewedFields());
        map.put("approvedFields", r.getApprovedFields());
        map.put("rejectedFields", r.getRejectedFields());
        map.put("reviewerId", r.getReviewerId());
        map.put("reviewedAt", r.getReviewedAt());
        map.put("createTime", r.getCreatedAt());
        map.put("updateTime", r.getUpdatedAt());

        // Document name
        map.put("documentName", reviewService.getDocumentNameMap(
                r.getDocumentId() != null ? Set.of(r.getDocumentId()) : Set.of())
                .getOrDefault(r.getDocumentId(), ""));

        // Reviewer name
        map.put("reviewer", reviewService.getUserNameMap(
                r.getReviewerId() != null ? Set.of(r.getReviewerId()) : Set.of())
                .getOrDefault(r.getReviewerId(), ""));

        // Review fields
        List<ReviewField> fields = reviewService.getFields(id);
        map.put("fields", fields.stream().map(f -> {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("id", f.getId());
            fm.put("fieldName", f.getFieldName());
            fm.put("fieldCode", f.getFieldCode());
            fm.put("extractedValue", f.getExtractedValue());
            fm.put("confidence", f.getConfidence());
            fm.put("status", f.getStatus());
            fm.put("correctedValue", f.getCorrectedValue());
            fm.put("comment", f.getComment());
            fm.put("imageRegion", f.getImageRegion());
            return fm;
        }).collect(Collectors.toList()));

        return ApiResponse.success(map);
    }

    @GetMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<List<Map<String, Object>>> getFields(@PathVariable Long id) {
        List<ReviewField> fields = reviewService.getFields(id);
        List<Map<String, Object>> records = fields.stream().map(f -> {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("id", f.getId());
            fm.put("fieldName", f.getFieldName());
            fm.put("fieldCode", f.getFieldCode());
            fm.put("extractedValue", f.getExtractedValue());
            fm.put("confidence", f.getConfidence());
            fm.put("status", f.getStatus());
            fm.put("correctedValue", f.getCorrectedValue());
            fm.put("comment", f.getComment());
            fm.put("imageRegion", f.getImageRegion());
            return fm;
        }).collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    @PostMapping("/{id}/fields/{fieldId}/approve")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> approveField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        ReviewField field = reviewService.handleFieldAction(id, fieldId, "approved", null, null, operatorId);
        if (field == null) return ApiResponse.error(404, "审核字段不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("status", field.getStatus());
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/fields/{fieldId}/reject")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> rejectField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        String comment = body != null ? (String) body.get("comment") : null;
        Long operatorId = getCurrentUserId(request);
        ReviewField field = reviewService.handleFieldAction(id, fieldId, "rejected", null, comment, operatorId);
        if (field == null) return ApiResponse.error(404, "审核字段不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("status", field.getStatus());
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/fields/{fieldId}/correct")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> correctField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String correctedValue = (String) body.get("correctedValue");
        Long operatorId = getCurrentUserId(request);
        ReviewField field = reviewService.handleFieldAction(id, fieldId, "corrected", correctedValue, null, operatorId);
        if (field == null) return ApiResponse.error(404, "审核字段不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("status", field.getStatus());
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/batch-approve")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Void> batchApprove(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        @SuppressWarnings("unchecked")
        List<Integer> fieldIds = (List<Integer>) body.getOrDefault("fieldIds", Collections.emptyList());
        reviewService.batchUpdateFields(id, fieldIds.stream().map(Long::valueOf).toList(), "approved", operatorId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/batch-reject")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Void> batchReject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        @SuppressWarnings("unchecked")
        List<Integer> fieldIds = (List<Integer>) body.getOrDefault("fieldIds", Collections.emptyList());
        reviewService.batchUpdateFields(id, fieldIds.stream().map(Long::valueOf).toList(), "rejected", operatorId);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable Long id) {
        List<ReviewHistory> histories = reviewService.getHistory(id);
        List<Map<String, Object>> records = histories.stream().map(h -> {
            Map<String, Object> hm = new LinkedHashMap<>();
            hm.put("id", h.getId());
            hm.put("fieldId", h.getFieldId());
            hm.put("action", h.getAction());
            hm.put("operatorId", h.getOperatorId());
            hm.put("detail", h.getDetail());
            hm.put("createTime", h.getCreatedAt());
            return hm;
        }).collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
        }
        return null;
    }
}
