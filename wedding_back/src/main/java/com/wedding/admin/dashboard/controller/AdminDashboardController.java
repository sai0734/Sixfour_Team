package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO;
import com.wedding.admin.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary")
    public AdminDashboardSummaryDTO getSummary() {
        return adminDashboardService.getSummary();
    }

}
