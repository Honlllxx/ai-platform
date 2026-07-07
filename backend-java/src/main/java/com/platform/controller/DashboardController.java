package com.platform.controller;

import com.platform.dto.ApiResponse;
import com.platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(dashboardService.getStats());
    }

    @GetMapping("/daily-processing")
    public ApiResponse<List<Map<String, Object>>> dailyProcessing() {
        return ApiResponse.success(dashboardService.getDailyProcessing());
    }

    @GetMapping("/doc-type-distribution")
    public ApiResponse<List<Map<String, Object>>> docTypeDistribution() {
        return ApiResponse.success(dashboardService.getDocTypeDistribution());
    }

    @GetMapping("/operation-trend")
    public ApiResponse<List<Map<String, Object>>> operationTrend() {
        return ApiResponse.success(dashboardService.getOperationTrend());
    }

    @GetMapping("/operation-type-distribution")
    public ApiResponse<List<Map<String, Object>>> operationTypeDistribution() {
        return ApiResponse.success(dashboardService.getOperationTypeDistribution());
    }
}
