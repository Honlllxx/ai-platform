package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.dto.ApiResponse;
import com.platform.dto.PageResult;
import com.platform.entity.business.Review;
import com.platform.entity.business.ReviewField;
import com.platform.entity.business.ReviewHistory;
import com.platform.mapper.business.ReviewFieldMapper;
import com.platform.mapper.business.ReviewHistoryMapper;
import com.platform.mapper.business.ReviewMapper;
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

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_review:list')")
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

        List<Map<String, Object>> records = reviewPage.getRecords().stream().map(r -> {
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
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(new PageResult<>(
                records, reviewPage.getTotal(), reviewPage.getCurrent(), reviewPage.getSize()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_review:list')")
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

    @PutMapping("/{id}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('ROLE_review:list')")
    public ApiResponse<Map<String, Object>> updateField(
            @PathVariable Long id,
            @PathVariable Long fieldId,
            @RequestBody Map<String, Object> body) {

        ReviewField field = reviewFieldMapper.selectById(fieldId);
        if (field == null) {
            return ApiResponse.error(404, "审核字段不存在");
        }

        String action = (String) body.getOrDefault("action", "approve");
        field.setStatus(action);
        if (body.containsKey("correctedValue")) {
            field.setCorrectedValue((String) body.get("correctedValue"));
        }
        if (body.containsKey("comment")) {
            field.setComment((String) body.get("comment"));
        }
        reviewFieldMapper.updateById(field);

        // Update review counts
        Review review = reviewMapper.selectById(id);
        if (review != null) {
            review.setReviewedFields(review.getReviewedFields() + 1);
            if ("approved".equals(action)) {
                review.setApprovedFields(review.getApprovedFields() + 1);
            } else if ("rejected".equals(action)) {
                review.setRejectedFields(review.getRejectedFields() + 1);
            }
            if (review.getReviewedFields() >= review.getTotalFields()) {
                review.setStatus("approved");
            }
            reviewMapper.updateById(review);
        }

        // Save history
        ReviewHistory history = new ReviewHistory();
        history.setReviewId(id);
        history.setFieldId(fieldId);
        history.setAction(action);
        history.setOperatorId(null);  // TODO: get from SecurityContext
        reviewHistoryMapper.insert(history);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("status", field.getStatus());
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/batch")
    @PreAuthorize("hasAuthority('ROLE_review:list')")
    public ApiResponse<Void> batchAction(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String action = (String) body.get("action");
        @SuppressWarnings("unchecked")
        List<Integer> fieldIds = (List<Integer>) body.getOrDefault("fieldIds", Collections.emptyList());

        for (Integer fid : fieldIds) {
            ReviewField field = reviewFieldMapper.selectById(fid.longValue());
            if (field != null) {
                field.setStatus(action);
                reviewFieldMapper.updateById(field);

                ReviewHistory history = new ReviewHistory();
                history.setReviewId(id);
                history.setFieldId(fid.longValue());
                history.setAction("batch_" + action);
                reviewHistoryMapper.insert(history);
            }
        }
        return ApiResponse.success();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('ROLE_review:list')")
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
}
