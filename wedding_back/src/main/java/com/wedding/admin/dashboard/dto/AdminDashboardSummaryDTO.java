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
