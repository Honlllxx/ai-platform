package com.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.annotation.OperationLog;
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

    // ==================== Stats ====================

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(reviewService.getStats());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> statistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long reviewerId) {
        return ApiResponse.success(reviewService.getStatistics(startDate, endDate, documentType, reviewerId));
    }

    // ==================== CRUD ====================

    @PostMapping
    @PreAuthorize("hasAuthority('review:create')")
    @OperationLog(value = "审核工作台", type = "新增")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Review r = reviewService.createReview(body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("status", r.getStatus());
        result.put("totalFields", r.getTotalFields());
        return ApiResponse.success(result);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String keyword) {

        Page<Review> reviewPage = reviewService.pageQuery(page, pageSize, status, documentType, keyword);

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
            map.put("documentName", r.getDocumentId() != null ? docNameMap.getOrDefault(r.getDocumentId(), "") : "");
            map.put("pipelineId", r.getPipelineId());
            map.put("documentType", r.getDocumentType());
            map.put("status", r.getStatus());
            map.put("totalFields", r.getTotalFields());
            map.put("reviewedFields", r.getReviewedFields());
            map.put("approvedFields", r.getApprovedFields());
            map.put("rejectedFields", r.getRejectedFields());
            map.put("reviewerId", r.getReviewerId());
            map.put("reviewer", r.getReviewerId() != null ? userNameMap.getOrDefault(r.getReviewerId(), "") : "");
            map.put("startedAt", r.getStartedAt());
            map.put("reviewedAt", r.getReviewedAt());
            map.put("reviewTime", r.getReviewedAt());
            map.put("reviewComment", r.getReviewComment());
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
        map.put("startedAt", r.getStartedAt());
        map.put("reviewedAt", r.getReviewedAt());
        map.put("reviewTime", r.getReviewedAt());
        map.put("reviewComment", r.getReviewComment());
        map.put("createTime", r.getCreatedAt());
        map.put("updateTime", r.getUpdatedAt());

        String docName = r.getDocumentId() != null ? reviewService.getDocumentNameMap(
                Set.of(r.getDocumentId())).getOrDefault(r.getDocumentId(), "") : "";
        map.put("documentName", docName);

        map.put("reviewer", r.getReviewerId() != null
                ? reviewService.getUserNameMap(Set.of(r.getReviewerId())).getOrDefault(r.getReviewerId(), "")
                : "");

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
            fm.put("originalImageRegion", f.getImageRegion());
            return fm;
        }).collect(Collectors.toList()));

        return ApiResponse.success(map);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('review:update')")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String status = (String) body.get("status");
        Long operatorId = getCurrentUserId(request);
        Review r = reviewService.updateStatus(id, status, operatorId);
        if (r == null) return ApiResponse.error(404, "审核记录不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("status", r.getStatus());
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('review:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean deleted = reviewService.deleteReview(id);
        return deleted ? ApiResponse.success() : ApiResponse.error(404, "审核记录不存在");
    }

    // ==================== Assignment ====================

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('review:update')")
    public ApiResponse<Map<String, Object>> assign(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long reviewerId = body.get("reviewerId") != null
                ? ((Number) body.get("reviewerId")).longValue() : null;
        if (reviewerId == null) {
            return ApiResponse.error(400, "reviewerId 不能为空");
        }
        Long operatorId = getCurrentUserId(request);
        Review r = reviewService.assignReviewer(id, reviewerId, operatorId);
        if (r == null) return ApiResponse.error(404, "审核记录不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("reviewerId", r.getReviewerId());
        result.put("reviewer", reviewService.getUserNameMap(Set.of(reviewerId)).getOrDefault(reviewerId, ""));
        return ApiResponse.success(result);
    }

    // ==================== Workflow ====================

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> startReview(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        Review r = reviewService.startReview(id, operatorId);
        if (r == null) return ApiResponse.error(404, "审核记录不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("status", r.getStatus());
        result.put("startedAt", r.getStartedAt());
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAuthority('review:update')")
    public ApiResponse<Map<String, Object>> saveComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String comment = (String) body.get("comment");
        Long operatorId = getCurrentUserId(request);
        Review r = reviewService.saveReviewComment(id, comment, operatorId);
        if (r == null) return ApiResponse.error(404, "审核记录不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", r.getId());
        result.put("reviewComment", r.getReviewComment());
        return ApiResponse.success(result);
    }

    // ==================== Fields ====================

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
            fm.put("originalImageRegion", f.getImageRegion());
            return fm;
        }).collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    @PostMapping("/{id}/fields/{fieldId}/approve")
    @PreAuthorize("hasAuthority('review:list')")
    @OperationLog(value = "审核工作台", type = "审核")
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
    @OperationLog(value = "审核工作台", type = "审核")
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
    @OperationLog(value = "审核工作台", type = "审核")
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
    @OperationLog(value = "审核工作台", type = "审核")
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
    @OperationLog(value = "审核工作台", type = "审核")
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

    @PostMapping("/{id}/batch-correct")
    @PreAuthorize("hasAuthority('review:list')")
    @OperationLog(value = "审核工作台", type = "审核")
    public ApiResponse<Void> batchCorrect(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long operatorId = getCurrentUserId(request);
        @SuppressWarnings("unchecked")
        List<Integer> fieldIds = (List<Integer>) body.getOrDefault("fieldIds", Collections.emptyList());
        String correctedValue = (String) body.get("correctedValue");
        reviewService.batchUpdateFieldsWithValue(id, fieldIds.stream().map(Long::valueOf).toList(), "corrected", correctedValue, operatorId);
        return ApiResponse.success();
    }

    // ==================== History ====================

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<PageResult<Map<String, Object>>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String action) {
        Page<ReviewHistory> historyPage = reviewService.getHistoryPage(id, page, pageSize, action);
        Map<Long, String> userNameMap = reviewService.getUserNameMap(
                historyPage.getRecords().stream().map(ReviewHistory::getOperatorId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()));
        // Build field name map for enriched history display
        Map<Long, String> fieldNameMap = reviewService.getFieldNameMap(
                historyPage.getRecords().stream().map(ReviewHistory::getFieldId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()));
        List<Map<String, Object>> records = historyPage.getRecords().stream().map(h -> {
            Map<String, Object> hm = new LinkedHashMap<>();
            hm.put("id", h.getId());
            hm.put("fieldId", h.getFieldId());
            hm.put("fieldName", h.getFieldId() != null ? fieldNameMap.getOrDefault(h.getFieldId(), "") : "");
            hm.put("action", h.getAction());
            hm.put("operatorId", h.getOperatorId());
            hm.put("operator", userNameMap.getOrDefault(h.getOperatorId(), ""));
            hm.put("detail", h.getDetail());
            // Parse detail JSON for frontend convenience
            hm.put("detailParsed", parseDetailJson(h.getAction(), h.getDetail()));
            hm.put("timestamp", h.getCreatedAt());
            hm.put("createTime", h.getCreatedAt());
            return hm;
        }).collect(Collectors.toList());
        return ApiResponse.success(new PageResult<>(
                records, historyPage.getTotal(), historyPage.getCurrent(), historyPage.getSize()));
    }

    private Map<String, Object> parseDetailJson(String action, String detail) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        if (detail != null && !detail.isBlank()) {
            try {
                // Simple JSON parsing for known detail format
                String content = detail.trim();
                if (content.startsWith("{")) {
                    content = content.substring(1, content.length() - 1);
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        String[] kv = pair.split(":", 2);
                        if (kv.length == 2) {
                            String key = kv[0].trim().replaceAll("\"", "");
                            String value = kv[1].trim().replaceAll("\"", "");
                            parsed.put(key, value);
                        }
                    }
                }
            } catch (Exception ignored) {
                parsed.put("raw", detail);
            }
        }
        return parsed;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
        }
        return null;
    }
}