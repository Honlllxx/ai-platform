package com.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.dto.ApiResponse;
import com.platform.entity.business.Review;
import com.platform.entity.business.ReviewHistory;
import com.platform.mapper.business.ReviewHistoryMapper;
import com.platform.mapper.business.ReviewMapper;
import com.platform.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ReviewHistoryMapper reviewHistoryMapper;
    private final ReviewMapper reviewMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ApiResponse.error(401, "未授权");
        }

        // Get reviews assigned to current user or reviewed by current user
        List<Review> userReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getReviewerId, currentUserId)
                        .orderByDesc(Review::getUpdatedAt)
                        .last("LIMIT 50"));

        if (userReviews.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }

        Set<Long> reviewIds = userReviews.stream()
                .map(Review::getId).collect(Collectors.toSet());

        // Get recent history for these reviews
        List<ReviewHistory> histories = reviewHistoryMapper.selectList(
                new LambdaQueryWrapper<ReviewHistory>()
                        .in(ReviewHistory::getReviewId, reviewIds)
                        .orderByDesc(ReviewHistory::getCreatedAt)
                        .last("LIMIT " + (limit * 2)));

        // Build notification list
        List<Map<String, Object>> notifications = new ArrayList<>();
        for (ReviewHistory h : histories) {
            Review r = userReviews.stream()
                    .filter(rv -> rv.getId().equals(h.getReviewId())).findFirst().orElse(null);
            if (r == null) continue;

            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", h.getId());
            n.put("reviewId", h.getReviewId());
            n.put("documentName", r.getDocumentId() != null ? "文档 #" + r.getDocumentId() : "未知文档");
            n.put("documentType", r.getDocumentType());
            n.put("action", h.getAction());
            n.put("actionLabel", getActionLabel(h.getAction()));
            n.put("detail", h.getDetail());
            n.put("timestamp", h.getCreatedAt());
            n.put("createTime", h.getCreatedAt());
            n.put("isRead", false); // TODO: track read status per user
            notifications.add(n);

            if (notifications.size() >= limit) break;
        }

        return ApiResponse.success(notifications);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> unreadCount(HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return ApiResponse.error(401, "未授权");
        }

        List<Review> userReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getReviewerId, currentUserId));

        long count = userReviews.stream()
                .filter(r -> "pending".equals(r.getStatus()) || "reviewing".equals(r.getStatus()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        // Also count new assignments
        long newAssignments = userReviews.stream()
                .filter(r -> "pending".equals(r.getStatus()))
                .count();
        result.put("newAssignments", newAssignments);
        result.put("inReview", userReviews.stream()
                .filter(r -> "reviewing".equals(r.getStatus())).count());

        return ApiResponse.success(result);
    }

    private String getActionLabel(String action) {
        return switch (action) {
            case "assign" -> "分配审核人";
            case "start_review" -> "开始审核";
            case "approved" -> "字段确认";
            case "rejected" -> "字段驳回";
            case "corrected" -> "字段修正";
            case "batch_approved" -> "批量通过";
            case "batch_rejected" -> "批量驳回";
            case "comment" -> "审核意见";
            case "status_change" -> "状态变更";
            default -> action;
        };
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
        }
        return null;
    }
}