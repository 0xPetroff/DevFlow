package com.devflow.dashboard.controller;

import com.devflow.dashboard.dto.DashboardResponse;
import com.devflow.dashboard.service.DashboardService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Aggregated figures across the caller's projects")
public class DashboardController {

    private static final int MAX_WINDOW_DAYS = 365;

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Project, issue and deployment totals for everything the caller can see")
    public DashboardResponse get(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestParam(defaultValue = "30") int windowDays) {
        return dashboardService.forUser(principal, Math.clamp(windowDays, 1, MAX_WINDOW_DAYS));
    }
}
