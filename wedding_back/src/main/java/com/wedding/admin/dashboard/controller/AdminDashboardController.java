package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.CompanyRankingItem;
import com.wedding.admin.dashboard.service.AdminDashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    // 대시보드 요약 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary")
    public AdminDashboardSummaryDTO getSummary() {
        return adminDashboardService.getSummary();
    }

    // "업체 매출 전체 순위" - categoryParam: "ALL" 또는 HALL/DRESS/STUDIO/MAKEUP, monthParam: "yyyy-MM" (없으면 이번 달)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/company-ranking")
    public List<CompanyRankingItem> getCompanyRanking(
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false) String month) {
        return adminDashboardService.getCompanyRanking(category, month);
    }

}
