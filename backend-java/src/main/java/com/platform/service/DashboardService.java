package com.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.entity.business.Review;
import com.platform.entity.document.Document;
import com.platform.entity.log.AgentTrace;
import com.platform.entity.log.OperationLog;
import com.platform.entity.pipeline.Pipeline;
import com.platform.entity.system.User;
import com.platform.mapper.business.ReviewMapper;
import com.platform.mapper.document.DocumentMapper;
import com.platform.mapper.log.AgentTraceMapper;
import com.platform.mapper.log.OperationLogMapper;
import com.platform.mapper.pipeline.PipelineMapper;
import com.platform.mapper.system.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final AgentTraceMapper agentTraceMapper;
    private final ReviewMapper reviewMapper;
    private final PipelineMapper pipelineMapper;
    private final DocumentMapper documentMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // User count
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().isNull(User::getDeletedAt));
        data.put("userCount", userCount);

        // Today's operations
        Long todayOperations = operationLogMapper.selectCount(
                new LambdaQueryWrapper<OperationLog>().ge(OperationLog::getCreatedAt, todayStart));
        data.put("todayOperations", todayOperations);

        // Agent calls today
        Long agentCalls = agentTraceMapper.selectCount(
                new LambdaQueryWrapper<AgentTrace>().ge(AgentTrace::getCreatedAt, todayStart));
        data.put("agentCalls", agentCalls);

        // Active agents (running status)
        Long activeAgents = agentTraceMapper.selectCount(
                new LambdaQueryWrapper<AgentTrace>().eq(AgentTrace::getStatus, "running"));
        data.put("activeAgents", activeAgents);

        // Auto pass rate from reviews
        Long totalApproved = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().gt(Review::getApprovedFields, 0));
        Long totalRejected = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().gt(Review::getRejectedFields, 0));
        long totalReviewed = totalApproved + totalRejected;
        double autoPassRate = totalReviewed > 0
                ? Math.round(totalApproved * 1000.0 / totalReviewed) / 10.0 : 0.0;
        data.put("autoPassRate", autoPassRate);
        data.put("manualCorrectionRate", totalReviewed > 0
                ? Math.round((100.0 - autoPassRate) * 10.0) / 10.0 : 0.0);

        // Cost savings
        long costSavings = totalApproved * 2;
        data.put("costSavings", costSavings);

        // Average processing time
        List<Pipeline> completedPipelines = pipelineMapper.selectList(
                new LambdaQueryWrapper<Pipeline>()
                        .eq(Pipeline::getStatus, "completed")
                        .isNotNull(Pipeline::getDuration));
        double avgProcessingTime = 0.0;
        if (!completedPipelines.isEmpty()) {
            avgProcessingTime = completedPipelines.stream()
                    .mapToDouble(p -> p.getDuration() != null ? p.getDuration() : 0.0)
                    .average()
                    .orElse(0.0);
            avgProcessingTime = Math.round(avgProcessingTime / 100.0) / 10.0;
        }
        data.put("avgProcessingTime", avgProcessingTime);

        return data;
    }

    public List<Map<String, Object>> getDailyProcessing() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            Long dayDocs = documentMapper.selectCount(
                    new LambdaQueryWrapper<Document>()
                            .ge(Document::getCreatedAt, dayStart)
                            .lt(Document::getCreatedAt, dayEnd));

            Long dayReviews = reviewMapper.selectCount(
                    new LambdaQueryWrapper<Review>()
                            .ge(Review::getCreatedAt, dayStart)
                            .lt(Review::getCreatedAt, dayEnd));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.toString().substring(5));
            item.put("autoPassed", dayDocs != null ? dayDocs : 0);
            item.put("manualCorrected", dayReviews != null ? dayReviews : 0);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getDocTypeDistribution() {
        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>().isNotNull(Document::getFileType));
        Map<String, Long> typeCount = docs.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getFileType() != null ? d.getFileType() : "未知",
                        Collectors.counting()));

        Map<String, String> typeLabels = Map.of(
                "pdf", "PDF", "image", "图片", "word", "Word", "excel", "Excel");

        return typeCount.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", typeLabels.getOrDefault(e.getKey(), e.getKey()));
            item.put("value", e.getValue());
            return item;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getOperationTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            Long count = operationLogMapper.selectCount(
                    new LambdaQueryWrapper<OperationLog>()
                            .ge(OperationLog::getCreatedAt, dayStart)
                            .lt(OperationLog::getCreatedAt, dayEnd));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.toString().substring(5));
            item.put("count", count != null ? count : 0);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getOperationTypeDistribution() {
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>().isNotNull(OperationLog::getOperationType));
        Map<String, Long> typeCount = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getOperationType() != null ? l.getOperationType() : "other",
                        Collectors.counting()));

        return typeCount.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getKey());
            item.put("count", e.getValue());
            return item;
        }).collect(Collectors.toList());
    }
}
