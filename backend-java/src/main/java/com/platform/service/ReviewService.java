package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewFieldMapper reviewFieldMapper;
    private final ReviewHistoryMapper reviewHistoryMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;

    // === Create ===

    @Transactional
    public Review createReview(Map<String, Object> body) {
        Review r = new Review();
        r.setDocumentId(body.get("documentId") != null
                ? ((Number) body.get("documentId")).longValue() : null);
        r.setPipelineId(body.get("pipelineId") != null
                ? ((Number) body.get("pipelineId")).longValue() : null);
        r.setDocumentType((String) body.get("documentType"));
        r.setReviewerId(body.get("reviewerId") != null
                ? ((Number) body.get("reviewerId")).longValue() : null);
        r.setStatus("pending");
        r.setTotalFields(0);
        r.setReviewedFields(0);
        r.setApprovedFields(0);
        r.setRejectedFields(0);
        reviewMapper.insert(r);
        return r;
    }

    // === Query ===

    public Page<Review> pageQuery(int page, int pageSize, String status, String documentType) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Review::getStatus, status);
        }
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(Review::getDocumentType, documentType);
        }
        wrapper.orderByDesc(Review::getCreatedAt);
        return reviewMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public Review getById(Long id) {
        return reviewMapper.selectById(id);
    }

    public Map<Long, String> getDocumentNameMap(Set<Long> docIds) {
        if (docIds.isEmpty()) return Map.of();
        List<Document> docs = documentMapper.selectBatchIds(docIds);
        return docs.stream().collect(Collectors.toMap(Document::getId, Document::getFileName));
    }

    public Map<Long, String> getUserNameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }

    // === Fields ===

    public List<ReviewField> getFields(Long reviewId) {
        return reviewFieldMapper.selectList(
                new LambdaQueryWrapper<ReviewField>().eq(ReviewField::getReviewId, reviewId));
    }

    public ReviewField getFieldById(Long fieldId) {
        return reviewFieldMapper.selectById(fieldId);
    }

    @Transactional
    public ReviewField handleFieldAction(Long reviewId, Long fieldId, String status,
                                          String correctedValue, String comment,
                                          Long operatorId) {
        ReviewField field = reviewFieldMapper.selectById(fieldId);
        if (field == null) return null;

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
        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setFieldId(fieldId);
        history.setAction(status);
        history.setOperatorId(operatorId);
        reviewHistoryMapper.insert(history);

        return field;
    }

    @Transactional
    public void batchUpdateFields(Long reviewId, List<Long> fieldIds, String status, Long operatorId) {
        for (Long fieldId : fieldIds) {
            ReviewField field = reviewFieldMapper.selectById(fieldId);
            if (field == null) continue;

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

    // === History ===

    public List<ReviewHistory> getHistory(Long reviewId) {
        return reviewHistoryMapper.selectList(
                new LambdaQueryWrapper<ReviewHistory>()
                        .eq(ReviewHistory::getReviewId, reviewId)
                        .orderByDesc(ReviewHistory::getCreatedAt));
    }
}
