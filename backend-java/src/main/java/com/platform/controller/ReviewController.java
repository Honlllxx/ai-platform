package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.Review;
import com.platform.entity.business.ReviewField;
import com.platform.entity.business.ReviewHistory;
import com.platform.entity.document.Document;
import com.platform.entity.system.User;
import com.platform.mapper.business.ReviewFieldMapper;
import com.platform.mapper.business.ReviewHistoryMapper;
import com.platform.mapper.business.ReviewMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.system.UserMapper;
import com.platform.security.JwtTokenProvider;
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

    private final ReviewMapper reviewMapper;
    private final ReviewFieldMapper reviewFieldMapper;
    private final ReviewHistoryMapper reviewHistoryMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType) {

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Review::getStatus, status);
        }
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(Review::getDocumentType, documentType);
        }
        wrapper.orderByDesc(Review::getCreatedAt);

        Page<Review> reviewPage = reviewMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // Batch fetch document names and reviewer names
        Set<Long> docIds = reviewPage.getRecords().stream()
                .map(Review::getDocumentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> reviewerIds = reviewPage.getRecords().stream()
                .map(Review::getReviewerId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> docNameMap = new HashMap<>();
        Map<Long, String> userNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<Document> docs = documentMapper.selectBatchIds(docIds);
            docNameMap = docs.stream().collect(Collectors.toMap(Document::getId, Document::getFileName));
        }
        if (!reviewerIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(reviewerIds);
            userNameMap = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        }

        Map<Long, String> finalDocNameMap = docNameMap;
        Map<Long, String> finalUserNameMap = userNameMap;
        List<Map<String, Object>> records = reviewPage.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("documentId", r.getDocumentId());
            map.put("documentName", finalDocNameMap.getOrDefault(r.getDocumentId(), ""));
            map.put("pipelineId", r.getPipelineId());
            map.put("documentType", r.getDocumentType());
            map.put("status", r.getStatus());
            map.put("totalFields", r.getTotalFields());
            map.put("reviewedFields", r.getReviewedFields());
            map.put("approvedFields", r.getApprovedFields());
            map.put("rejectedFields", r.getRejectedFields());
            map.put("reviewerId", r.getReviewerId());
            map.put("reviewer", finalUserNameMap.getOrDefault(r.getReviewerId(), ""));
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
        Review r = reviewMapper.selectById(id);
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
        if (r.getDocumentId() != null) {
            Document doc = documentMapper.selectById(r.getDocumentId());
            map.put("documentName", doc != null ? doc.getFileName() : "");
        } else {
            map.put("documentName", "");
        }
        // Reviewer name
        if (r.getReviewerId() != null) {
            User reviewer = userMapper.selectById(r.getReviewerId());
            map.put("reviewer", reviewer != null ? reviewer.getUsername() : "");
        } else {
            map.put("reviewer", "");
        }

        // Review fields
        List<ReviewField> fields = reviewFieldMapper.selectList(
                new LambdaQueryWrapper<ReviewField>().eq(ReviewField::getReviewId, id));
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
        List<ReviewField> fields = reviewFieldMapper.selectList(
                new LambdaQueryWrapper<ReviewField>().eq(ReviewField::getReviewId, id));
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
        return handleFieldAction(id, fieldId, "approved", null, null, request);
    }

    @PostMapping("/{id}/fields/{fieldId}/reject")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> rejectField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        String comment = body != null ? (String) body.get("comment") : null;
        return handleFieldAction(id, fieldId, "rejected", null, comment, request);
    }

    @PostMapping("/{id}/fields/{fieldId}/correct")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<Map<String, Object>> correctField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String correctedValue = (String) body.get("correctedValue");
        return handleFieldAction(id, fieldId, "corrected", correctedValue, null, request);
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
        for (Integer fid : fieldIds) {
            updateFieldStatus(id, fid.longValue(), "approved", operatorId);
        }
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
        for (Integer fid : fieldIds) {
            updateFieldStatus(id, fid.longValue(), "rejected", operatorId);
        }
        return ApiResponse.success();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('review:list')")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable Long id) {
        List<ReviewHistory> histories = reviewHistoryMapper.selectList(
                new LambdaQueryWrapper<ReviewHistory>()
                        .eq(ReviewHistory::getReviewId, id)
                        .orderByDesc(ReviewHistory::getCreatedAt));

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

    // === Private helpers ===

    private ApiResponse<Map<String, Object>> handleFieldAction(
            Long reviewId, Long fieldId, String status,
            String correctedValue, String comment, HttpServletRequest request) {
        ReviewField field = reviewFieldMapper.selectById(fieldId);
        if (field == null) {
            return ApiResponse.error(404, "审核字段不存在");
        }

        field.setStatus(status);
        if (correctedValue != null) {
            field.setCorrectedValue(correctedValue);
        }
        if (comment != null) {
            field.setComment(comment);
        }
        reviewFieldMapper.updateById(field);

        // Update review counts
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setReviewedFields(review.getReviewedFields() + 1);
            if ("approved".equals(status)) {
                review.setApprovedFields(review.getApprovedFields() + 1);
            } else if ("rejected".equals(status)) {
                review.setRejectedFields(review.getRejectedFields() + 1);
            }
            if (review.getReviewedFields() >= review.getTotalFields()) {
                review.setStatus("approved");
            }
            reviewMapper.updateById(review);
        }

        // Save history
        Long operatorId = getCurrentUserId(request);
        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setFieldId(fieldId);
        history.setAction(status);
        history.setOperatorId(operatorId);
        reviewHistoryMapper.insert(history);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("status", field.getStatus());
        return ApiResponse.success(result);
    }

    private void updateFieldStatus(Long reviewId, Long fieldId, String status, Long operatorId) {
        ReviewField field = reviewFieldMapper.selectById(fieldId);
        if (field != null) {
            field.setStatus(status);
            reviewFieldMapper.updateById(field);

            ReviewHistory history = new ReviewHistory();
            history.setReviewId(reviewId);
            history.setFieldId(fieldId);
            history.setAction("batch_" + status);
            history.setOperatorId(operatorId);
            reviewHistoryMapper.insert(history);
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
        }
        return null;
    }
}
