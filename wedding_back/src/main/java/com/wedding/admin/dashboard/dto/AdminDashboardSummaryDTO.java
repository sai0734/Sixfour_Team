package com.wedding.admin.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// GET /api/admin/dashboard/summary 응답 - 관리자가 매일 확인해야 할 전체 현황을 한 번에 묶어서 내려줌.
// 도메인별로 이미 있는 화면(회원관리/주문관리/상품관리 등)의 숫자 요약 + "오늘 할 일" todo 리스트로 구성됨.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardSummaryDTO {

    private MemberStats memberStats;
    private OrderStats orderStats;
    private BoardStats boardStats;
    private ReservationStats reservationStats;
    private InquiryStats inquiryStats;
    private ProductStats productStats;
    // 업체 현황 카드 4개 - 웨딩홀/드레스/스튜디오/메이크업 순서로 카테고리별 매출 1위·평균·꼴찌
    private List<CategoryRevenueCard> categoryRevenueCards;
    // 업체 현황 "월별 매출 추이" 드롭다운용 - ALL/HALL/DRESS/STUDIO/MAKEUP 5개, 각각 최근 6개월치
    private List<CategoryMonthlyRevenue> companyMonthlyRevenueByCategory;

    // 최근 6개월 매출 추이 (오래된 달 -> 최신 달 순)
    private List<MonthlyRevenuePoint> monthlyRevenue;

    // 관리자가 오늘 처리해야 할 액션 아이템 (개수 기준 내림차순 정렬은 프론트에서)
    private List<TodoItem> todos;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberStats {
        private long total;
        private long active;
        private long dormant;
        private long blacklist;
        private long withdrawn;
        private long newToday;
        private long unverifiedEmail;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderStats {
        private long paid;
        private long shipping;
        private long delivered;
        private long todayCount;
        private long totalRevenue;
        private long todayRevenue;

        // 이번 달 vs 지난 달 매출 비교
        private long currentMonthRevenue;
        private long lastMonthRevenue;
        private long revenueChangeAmount; // currentMonthRevenue - lastMonthRevenue
        private Double revenueChangeRate; // (증감액 / 지난달 매출) * 100, 지난달 매출 0이면 null
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyRevenuePoint {
        private String month; // "2026-07" 형식
        private long revenue;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoardStats {
        private long total;
        private long freeCount;
        private long reviewCount;
        private long todayCount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReservationStats {
        private long total;
        private long pending; // status = "대기"
        private long paidCount; // payStatus = "PAID"
        private long weddingThisWeek; // weddingDate가 이번 주(오늘~+7일)인 예약
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InquiryStats {
        private long openRooms;
        private long closedRooms;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductStats {
        private long total;
        private long lowStockCount; // stockQty <= 5 (품절 포함)
        private List<LowStockProduct> lowStockProducts;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LowStockProduct {
        private Long pno;
        private String pname;
        private int stockQty;
    }

    // 업체 카테고리 1개(웨딩홀 등) 기준 - 결제완료 예약 금액 합산 매출 1위/평균/꼴찌
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryRevenueCard {
        private String category; // HALL / DRESS / STUDIO / MAKEUP
        private String categoryLabel; // "웨딩홀" 등
        private String topCompanyName;
        private long topAmount;
        private long averageAmount;
        private String bottomCompanyName;
        private long bottomAmount;
    }

    // "업체 매출 전체 순위" 카드 1행 - 특정 월(카테고리 필터 가능) 매출 기준 랭킹 + 전달 대비 순위 변동
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyRankingItem {
        private int rank;
        private String companyName;
        private String category; // HALL / DRESS / STUDIO / MAKEUP
        private String categoryLabel; // "웨딩홀" 등
        private long amount;
        // 전달 순위 - 이번 달 순위 (양수: 순위 상승, 음수: 순위 하락, null: 전달에 매출 없어 비교 불가/신규)
        private Integer rankChange;
    }

    // "업체 월별 매출 추이" 드롭다운 1개 옵션 (전체 또는 카테고리 1개) + 그 옵션의 6개월치 추이
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryMonthlyRevenue {
        private String category; // "ALL" / HALL / DRESS / STUDIO / MAKEUP
        private String categoryLabel; // "전체" / "웨딩홀" 등
        private List<MonthlyRevenuePoint> monthlyRevenue;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TodoItem {
        private String label;
        private long count;
        // info / warning / danger - 프론트에서 색상 매핑용
        private String tone;
        // 프론트 라우팅용 (예: "/admin/orders?status=PAID")
        private String link;
        // 카드 하나에 여러 세부 항목을 같이 보여줄 때 사용 (없으면 null)
        private List<TodoBreakdownItem> breakdown;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TodoBreakdownItem {
        private String label;
        private long count;
    }
}
