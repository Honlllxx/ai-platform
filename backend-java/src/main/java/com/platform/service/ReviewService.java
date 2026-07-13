package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.entity.business.Review;
import com.platform.entity.business.ReviewField;
import com.platform.entity.business.ReviewHistory;
import com.platform.entity.document.Document;
import com.platform.entity.system.User;
import com.platform.entity.pipeline.ExtractionResult;
import com.platform.entity.pipeline.Pipeline;
import com.platform.mapper.business.ReviewFieldMapper;
import com.platform.mapper.business.ReviewHistoryMapper;
import com.platform.mapper.business.ReviewMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.pipeline.ExtractionResultMapper;
import com.platform.mapper.pipeline.PipelineMapper;
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
    private final ExtractionResultMapper extractionResultMapper;
    private final PipelineMapper pipelineMapper;

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
        } else {
            // Auto-import fields from extraction_results when pipelineId is provided
            Long pipelineId = r.getPipelineId();
            // If no pipelineId, try to find the latest pipeline for this document
            if (pipelineId == null && r.getDocumentId() != null) {
                List<Pipeline> docPipelines = pipelineMapper.selectList(
                        new LambdaQueryWrapper<Pipeline>()
                                .eq(Pipeline::getDocumentId, r.getDocumentId())
                                .orderByDesc(Pipeline::getCreatedAt)
                                .last("LIMIT 1"));
                if (!docPipelines.isEmpty()) {
                    pipelineId = docPipelines.get(0).getId();
                    r.setPipelineId(pipelineId);
                    reviewMapper.updateById(r);
                }
            }
            if (pipelineId != null) {
                List<ExtractionResult> extractions = extractionResultMapper.selectList(
                        new LambdaQueryWrapper<ExtractionResult>()
                                .eq(ExtractionResult::getPipelineId, pipelineId));
                if (!extractions.isEmpty()) {
                    for (ExtractionResult er : extractions) {
                        ReviewField rf = new ReviewField();
                        rf.setReviewId(r.getId());
                        rf.setExtractionId(er.getId());
                        rf.setFieldName(er.getFieldName());
                        rf.setFieldCode(er.getFieldCode());
                        rf.setExtractedValue(er.getExtractedValue());
                        rf.setConfidence(er.getConfidence() != null ? er.getConfidence() : 0.0);
                        rf.setStatus("pending");
                        reviewFieldMapper.insert(rf);
                    }
                    r.setTotalFields(extractions.size());
                    reviewMapper.updateById(r);
                }
            }
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

    public Map<Long, String> getFieldNameMap(Set<Long> fieldIds) {
        if (fieldIds.isEmpty()) return Map.of();
        List<ReviewField> fields = reviewFieldMapper.selectBatchIds(fieldIds);
        return fields.stream().collect(Collectors.toMap(ReviewField::getId, ReviewField::getFieldName));
    }

    // === Stats ===

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Pending count
        Long pendingCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getStatus, "pending"));
        stats.put("pendingCount", pendingCount);

        // Reviewing count
        Long reviewingCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getStatus, "reviewing"));
        stats.put("reviewingCount", reviewingCount);

        // Today reviewed
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayReviewed = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .ne(Review::getStatus, "pending")
                        .ge(Review::getReviewedAt, todayStart)
                        .le(Review::getReviewedAt, todayEnd));
        stats.put("todayReviewed", todayReviewed);

        // Approval rate — count from review_fields table for accuracy
        List<ReviewField> allFields = reviewFieldMapper.selectList(null);
        long totalFieldsCount = allFields.size();
        long totalApprovedCount = allFields.stream()
                .filter(f -> "approved".equals(f.getStatus()) || "corrected".equals(f.getStatus()))
                .count();
        long totalRejectedCount = allFields.stream()
                .filter(f -> "rejected".equals(f.getStatus()))
                .count();
        long totalReviewedCount = totalApprovedCount + totalRejectedCount;
        int approvalRate = totalReviewedCount > 0
                ? (int) Math.round((double) totalApprovedCount / totalReviewedCount * 100) : 0;
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
    public Review startReview(Long reviewId, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return null;
        if (!"pending".equals(review.getStatus())) return review;

        review.setStatus("reviewing");
        review.setStartedAt(LocalDateTime.now());
        reviewMapper.updateById(review);

        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setAction("start_review");
        history.setOperatorId(operatorId);
        history.setDetail("{\"from\":\"pending\",\"to\":\"reviewing\"}");
        reviewHistoryMapper.insert(history);

        return review;
    }

    @Transactional
    public Review saveReviewComment(Long reviewId, String comment, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return null;

        review.setReviewComment(comment);
        reviewMapper.updateById(review);

        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setAction("comment");
        history.setOperatorId(operatorId);
        history.setDetail("{\"comment\":\"" + (comment != null ? comment.replace("\"", "'") : "") + "\"}");
        reviewHistoryMapper.insert(history);

        return review;
    }

    @Transactional
    public Review updateStatus(Long reviewId, String status, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) return null;

        String oldStatus = review.getStatus();
        review.setStatus(status);
        if ("approved".equals(status) || "rejected".equals(status)) {
            review.setReviewedAt(LocalDateTime.now());
        }
        reviewMapper.updateById(review);

        ReviewHistory history = new ReviewHistory();
        history.setReviewId(reviewId);
        history.setAction("status_change");
        history.setOperatorId(operatorId);
        history.setDetail("{\"from\":\"" + oldStatus + "\",\"to\":\"" + status + "\"}");
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
            // Auto-transition to "reviewing" on first field action
            if (wasPending && "pending".equals(review.getStatus())) {
                review.setStatus("reviewing");
                review.setStartedAt(LocalDateTime.now());
            }
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
        batchUpdateFieldsInternal(reviewId, fieldIds, status, null, operatorId);
    }

    @Transactional
    public void batchUpdateFieldsWithValue(Long reviewId, List<Long> fieldIds, String status,
                                            String correctedValue, Long operatorId) {
        batchUpdateFieldsInternal(reviewId, fieldIds, status, correctedValue, operatorId);
    }

    private void batchUpdateFieldsInternal(Long reviewId, List<Long> fieldIds, String status,
                                            String correctedValue, Long operatorId) {
        Review review = reviewMapper.selectById(reviewId);
        int count = 0;

        for (Long fieldId : fieldIds) {
            ReviewField field = reviewFieldMapper.selectById(fieldId);
            if (field == null) continue;

            String oldStatus = field.getStatus();
            field.setStatus(status);
            if (correctedValue != null) {
                field.setCorrectedValue(correctedValue);
            }
            reviewFieldMapper.updateById(field);

            if ("pending".equals(oldStatus)) {
                count++;
            }

            ReviewHistory history = new ReviewHistory();
            history.setReviewId(reviewId);
            history.setFieldId(fieldId);
            history.setAction("batch_" + status);
            history.setOperatorId(operatorId);
            history.setDetail("{\"fieldName\":\"" + field.getFieldName() + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + status + "\"}");
            reviewHistoryMapper.insert(history);
        }

        // Update review counts
        if (review != null && count > 0) {
            // Auto-transition to "reviewing" on first batch action
            if ("pending".equals(review.getStatus())) {
                review.setStatus("reviewing");
                review.setStartedAt(LocalDateTime.now());
            }
            review.setReviewedFields(review.getReviewedFields() + count);
            if ("approved".equals(status) || "corrected".equals(status)) {
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

    public Page<ReviewHistory> getHistoryPage(Long reviewId, int page, int pageSize, String action) {
        LambdaQueryWrapper<ReviewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewHistory::getReviewId, reviewId);
        if (action != null && !action.isBlank()) {
            wrapper.eq(ReviewHistory::getAction, action);
        }
        wrapper.orderByDesc(ReviewHistory::getCreatedAt);
        return reviewHistoryMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    // === Enhanced Statistics ===

    public Map<String, Object> getStatistics(String startDate, String endDate, String documentType, Long reviewerId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(Review::getCreatedAt, LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIN));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(Review::getCreatedAt, LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX));
        }
        if (documentType != null && !documentType.isBlank()) {
            wrapper.eq(Review::getDocumentType, documentType);
        }
        if (reviewerId != null) {
            wrapper.eq(Review::getReviewerId, reviewerId);
        }

        List<Review> reviews = reviewMapper.selectList(wrapper);

        // Status distribution
        long pendingCount = reviews.stream().filter(r -> "pending".equals(r.getStatus())).count();
        long reviewingCount = reviews.stream().filter(r -> "reviewing".equals(r.getStatus())).count();
        long approvedCount = reviews.stream().filter(r -> "approved".equals(r.getStatus())).count();
        long rejectedCount = reviews.stream().filter(r -> "rejected".equals(r.getStatus())).count();
        long partialCount = reviews.stream().filter(r -> "partial".equals(r.getStatus())).count();

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("pending", pendingCount);
        statusDistribution.put("reviewing", reviewingCount);
        statusDistribution.put("approved", approvedCount);
        statusDistribution.put("rejected", rejectedCount);
        statusDistribution.put("partial", partialCount);
        stats.put("statusDistribution", statusDistribution);

        // Document type distribution
        Map<String, Long> docTypeDistribution = reviews.stream()
                .filter(r -> r.getDocumentType() != null && !r.getDocumentType().isBlank())
                .collect(Collectors.groupingBy(Review::getDocumentType, LinkedHashMap::new, Collectors.counting()));
        stats.put("docTypeDistribution", docTypeDistribution);

        // Reviewer performance
        Set<Long> reviewerIds = reviews.stream()
                .map(Review::getReviewerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> reviewerNames = getUserNameMap(reviewerIds);
        List<Map<String, Object>> reviewerStats = reviews.stream()
                .filter(r -> r.getReviewerId() != null)
                .collect(Collectors.groupingBy(Review::getReviewerId))
                .entrySet().stream().map(e -> {
                    Map<String, Object> rs = new LinkedHashMap<>();
                    rs.put("reviewerId", e.getKey());
                    rs.put("reviewer", reviewerNames.getOrDefault(e.getKey(), ""));
                    rs.put("totalReviews", e.getValue().size());
                    rs.put("approvedCount", e.getValue().stream().filter(r -> "approved".equals(r.getStatus())).count());
                    rs.put("rejectedCount", e.getValue().stream().filter(r -> "rejected".equals(r.getStatus())).count());
                    rs.put("partialCount", e.getValue().stream().filter(r -> "partial".equals(r.getStatus())).count());
                    return rs;
                }).collect(Collectors.toList());
        stats.put("reviewerStats", reviewerStats);

        // Daily trend (last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Review> recentReviews = reviews.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo.minusDays(1)))
                .collect(Collectors.toList());
        Map<LocalDate, Long> dailyCounts = recentReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> dailyCompleted = recentReviews.stream()
                .filter(r -> "approved".equals(r.getStatus()) || "rejected".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                .collect(Collectors.groupingBy(r -> r.getReviewedAt() != null ? r.getReviewedAt().toLocalDate() : r.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 30; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("created", dailyCounts.getOrDefault(date, 0L));
            day.put("completed", dailyCompleted.getOrDefault(date, 0L));
            dailyTrend.add(day);
        }
        stats.put("dailyTrend", dailyTrend);

        // Summary
        stats.put("totalCount", reviews.size());
        long totalFields = reviews.stream().mapToLong(r -> r.getTotalFields() != null ? r.getTotalFields() : 0).sum();
        long totalApproved = reviews.stream().mapToLong(r -> r.getApprovedFields() != null ? r.getApprovedFields() : 0).sum();
        stats.put("totalFields", totalFields);
        stats.put("totalApproved", totalApproved);
        stats.put("overallApprovalRate", totalFields > 0 ? Math.round((double) totalApproved / totalFields * 100) : 0);

        return stats;
    }
}