package com.wedding.admin.dashboard.service;

import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.CompanyRankingItem;
import java.util.List;

public interface AdminDashboardService {

    AdminDashboardSummaryDTO getSummary();

    // "업체 매출 전체 순위" - categoryParam: "ALL" 또는 HALL/DRESS/STUDIO/MAKEUP, monthParam: "yyyy-MM" (없으면 이번 달)
    List<CompanyRankingItem> getCompanyRanking(String categoryParam, String monthParam);

}
