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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // Auto-populate review fields from request body if provided
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) body.get("fields");
        if (fields != null && !fields.isEmpty()) {
            for (Map<String, Object> f : fields) {
                ReviewField rf = new ReviewField();
                rf.setReviewId(r.getId());
                rf.setFieldName((String) f.get("fieldName"));
                rf.setFieldCode((String) f.get("fieldCode"));
                rf.setExtractedValue((String) f.get("extractedValue"));
                rf.setConfidence(f.get("confidence") != null
                        ? ((Number) f.get("confidence")).doubleValue() : 0.0);
                rf.setImageRegion((String) f.get("imageRegion"));
                rf.setStatus("pending");
                reviewFieldMapper.insert(rf);
            }
            r.setTotalFields(fields.size());
            reviewMapper.updateById(r);
        }

        return r;
    }

    // === Query ===

    public Page<Review> pageQuery(int page, int pageSize, String status, String documentType, String keyword) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Review::getStatus, status);
        }
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(Review::getDocumentType, documentType);
        }
        if (keyword != null && !keyword.isBlank()) {
            // Search by document name: find matching document IDs first
            List<Document> matchingDocs = documentMapper.selectList(
                    new LambdaQueryWrapper<Document>()
                            .like(Document::getFileName, keyword));
            Set<Long> matchingDocIds = matchingDocs.stream()
                    .map(Document::getId).collect(Collectors.toSet());
            if (!matchingDocIds.isEmpty()) {
                wrapper.in(Review::getDocumentId, matchingDocIds);
            } else {
                // No matching documents, return empty result
                wrapper.eq(Review::getId, -1L);
            }
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

    // === Stats ===

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Pending count
        Long pendingCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getStatus, "pending"));
        stats.put("pendingCount", pendingCount);

        // Today reviewed
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayReviewed = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .ne(Review::getStatus, "pending")
                        .ge(Review::getReviewedAt, todayStart)
                        .le(Review::getReviewedAt, todayEnd));
        stats.put("todayReviewed", todayReviewed);

        // Approval rate (total approved fields / total reviewed fields)
        List<Review> allReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .ne(Review::getStatus, "pending"));
        long totalFields = allReviews.stream().mapToLong(r -> r.getTotalFields() != null ? r.getTotalFields() : 0).sum();
        long totalApproved = allReviews.stream().mapToLong(r -> r.getApprovedFields() != null ? r.getApprovedFields() : 0).sum();
        int approvalRate = totalFields > 0 ? (int) Math.round((double) totalApproved / totalFields * 100) : 0;
        stats.put("approvalRate", approvalRate);

        // Total reviews
        Long totalReviews = reviewMapper.selectCount(null);
        stats.put("totalReviews", totalReviews);

        return stats;
    }

    // === Assignment ===

    @Transactional
    public Review assignReviewer(Long reviewId, Long reviewerId, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return null;

        review.setReviewerId(reviewerId);
        reviewMapper.updateById(review);

        // Record assignment in history
        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setAction("assign");
        history.setOperatorId(operatorId);
        String reviewerName = userNameMap(Set.of(reviewerId)).getOrDefault(reviewerId, String.valueOf(reviewerId));
        history.setDetail("{\"reviewerId\":" + reviewerId + ",\"reviewer\":\"" + reviewerName + "\"}");
        reviewHistoryMapper.insert(history);

        return review;
    }

    private Map<Long, String> userNameMap(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }

    // === Status Update ===

    @Transactional
    public Review updateStatus(Long reviewId, String status, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return null;

        review.setStatus(status);
        review.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(review);

        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setAction("status_change");
        history.setOperatorId(operatorId);
        history.setDetail("{\"from\":\"" + (review.getStatus() != null ? review.getStatus() : "pending") + "\",\"to\":\"" + status + "\"}");
        reviewHistoryMapper.insert(history);

        return review;
    }

    // === Delete ===

    @Transactional
    public boolean deleteReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return false;
        // Soft delete via @TableLogic
        reviewMapper.deleteById(reviewId);
        return true;
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

        String oldStatus = field.getStatus();
        boolean wasPending = "pending".equals(oldStatus);

        field.setStatus(status);
        if (correctedValue != null) {
            field.setCorrectedValue(correctedValue);
        }
        if (comment != null) {
            field.setComment(comment);
        }
        reviewFieldMapper.updateById(field);

        // Update review counts — only increment when transitioning from pending
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            if (wasPending) {
                review.setReviewedFields(review.getReviewedFields() + 1);
                if ("approved".equals(status) || "corrected".equals(status)) {
                    review.setApprovedFields(review.getApprovedFields() + 1);
                } else if ("rejected".equals(status)) {
                    review.setRejectedFields(review.getRejectedFields() + 1);
                }
            } else {
                // If status changed from non-pending, adjust counts
                if ("approved".equals(oldStatus) || "corrected".equals(oldStatus)) {
                    review.setApprovedFields(Math.max(0, review.getApprovedFields() - 1));
                } else if ("rejected".equals(oldStatus)) {
                    review.setRejectedFields(Math.max(0, review.getRejectedFields() - 1));
                }
                if ("approved".equals(status) || "corrected".equals(status)) {
                    review.setApprovedFields(review.getApprovedFields() + 1);
                } else if ("rejected".equals(status)) {
                    review.setRejectedFields(review.getRejectedFields() + 1);
                }
            }
            // Auto-complete: if all fields reviewed, update review status
            if (review.getReviewedFields() >= review.getTotalFields()) {
                review.setStatus(review.getRejectedFields() == 0 ? "approved" : "partial");
                review.setReviewedAt(LocalDateTime.now());
            } else if (review.getReviewedFields() > 0) {
                review.setStatus("partial");
            }
            reviewMapper.updateById(review);
        }

        // Save history
        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setFieldId(fieldId);
        history.setAction(status);
        history.setOperatorId(operatorId);
        history.setDetail("{\"fieldName\":\"" + field.getFieldName() + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + status + "\"}");
        reviewHistoryMapper.insert(history);

        return field;
    }

    @Transactional
    public void batchUpdateFields(Long reviewId, List<Long> fieldIds, String status, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        int count = 0;

        for (Long fieldId : fieldIds) {
            ReviewField field = reviewFieldMapper.selectById(fieldId);
            if (field == null) continue;

            String oldStatus = field.getStatus();
            field.setStatus(status);
            reviewFieldMapper.updateById(field);

            if ("pending".equals(oldStatus)) {
                count++;
            }

            ReviewHistory history = new ReviewHistory();
            history.setReviewId(reviewId);
            history.setFieldId(fieldId);
            history.setAction("batch_" + status);
            history.setOperatorId(operatorId);
            reviewHistoryMapper.insert(history);
        }

        // Update review counts
        if (review != null && count > 0) {
            review.setReviewedFields(review.getReviewedFields() + count);
            if ("approved".equals(status)) {
                review.setApprovedFields(review.getApprovedFields() + count);
            } else if ("rejected".equals(status)) {
                review.setRejectedFields(review.getRejectedFields() + count);
            }
            if (review.getReviewedFields() >= review.getTotalFields()) {
                review.setStatus(review.getRejectedFields() == 0 ? "approved" : "partial");
                review.setReviewedAt(LocalDateTime.now());
            } else if (review.getReviewedFields() > 0) {
                review.setStatus("partial");
            }
            reviewMapper.updateById(review);
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