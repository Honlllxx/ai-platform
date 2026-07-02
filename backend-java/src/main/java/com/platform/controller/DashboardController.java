package com.platform.controller;

import com.platform.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", 0);
        data.put("todayOperations", 0);
        data.put("agentCalls", 0);
        data.put("activeAgents", 0);
        data.put("autoPassRate", 0.0);
        data.put("manualCorrectionRate", 0.0);
        data.put("costSavings", 0);
        data.put("avgProcessingTime", 0.0);
        return ApiResponse.success(data);
    }

    @GetMapping("/daily-processing")
    public ApiResponse<List<Map<String, Object>>> dailyProcessing() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/doc-type-distribution")
    public ApiResponse<List<Map<String, Object>>> docTypeDistribution() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/operation-trend")
    public ApiResponse<List<Map<String, Object>>> operationTrend() {
        return ApiResponse.success(List.of());
    }

    @GetMapping("/operation-type-distribution")
    public ApiResponse<List<Map<String, Object>>> operationTypeDistribution() {
        return ApiResponse.success(List.of());
    }
}
