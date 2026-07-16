package com.wedding.admin.dashboard.service;

import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.BoardStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.InquiryStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.LowStockProduct;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.MemberStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.MonthlyRevenuePoint;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.OrderStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.ProductStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.ReservationStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.TodoItem;
import com.wedding.board.repository.BoardRepository;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.inquiry.domain.InquiryRoomStatus;
import com.wedding.inquiry.repository.InquiryRoomRepository;
import com.wedding.member.repository.MemberRepository;
import com.wedding.product.domain.Product;
import com.wedding.product.repository.ProductRepository;
import com.wedding.product.repository.QnaRepository;
import com.wedding.reservation.repository.ReservationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final BoardRepository boardRepository;
    private final ReservationRepository reservationRepository;
    private final InquiryRoomRepository inquiryRoomRepository;
    private final ProductRepository productRepository;
    private final QnaRepository qnaRepository;

    @Override
    public AdminDashboardSummaryDTO getSummary() {

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Map<YearMonth, Long> monthlyRevenueMap = buildMonthlyRevenueMap();

        MemberStats memberStats = buildMemberStats(todayStart);
        OrderStats orderStats = buildOrderStats(todayStart, monthlyRevenueMap);
        BoardStats boardStats = buildBoardStats(todayStart);
        ReservationStats reservationStats = buildReservationStats();
        InquiryStats inquiryStats = buildInquiryStats();
        ProductStats productStats = buildProductStats();

        List<TodoItem> todos = buildTodos(orderStats, productStats);

        return AdminDashboardSummaryDTO.builder()
                .memberStats(memberStats)
                .orderStats(orderStats)
                .boardStats(boardStats)
                .reservationStats(reservationStats)
                .inquiryStats(inquiryStats)
                .productStats(productStats)
                .monthlyRevenue(toMonthlyRevenuePoints(monthlyRevenueMap))
                .todos(todos)
                .build();
    }

    // 최근 6개월(이번 달 포함) 매출을 월별로 묶어서 Map으로 반환 (오래된 달 -> 최신 달 순서 보장)
    private Map<YearMonth, Long> buildMonthlyRevenueMap() {
        int monthsBack = 5; // 이번 달 포함 총 6개월
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(monthsBack);
        LocalDateTime since = startMonth.atDay(1).atStartOfDay();

        Map<YearMonth, Long> monthlyRevenue = new LinkedHashMap<>();
        for (int i = 0; i <= monthsBack; i++) {
            monthlyRevenue.put(startMonth.plusMonths(i), 0L);
        }

        List<Object[]> rows = orderRepository.findRevenueRowsSince(since);
        for (Object[] row : rows) {
            LocalDateTime regDate = (LocalDateTime) row[0];
            Number totalPrice = (Number) row[1];
            YearMonth month = YearMonth.from(regDate);

            monthlyRevenue.merge(month, totalPrice.longValue(), Long::sum);
        }

        return monthlyRevenue;
    }

    private List<MonthlyRevenuePoint> toMonthlyRevenuePoints(Map<YearMonth, Long> monthlyRevenueMap) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        return monthlyRevenueMap.entrySet().stream()
                .map(entry -> MonthlyRevenuePoint.builder()
                        .month(entry.getKey().format(formatter))
                        .revenue(entry.getValue())
                        .build())
                .toList();
    }

    private MemberStats buildMemberStats(LocalDateTime todayStart) {
        return MemberStats.builder()
                .total(memberRepository.count())
                .active(memberRepository.countByStatus("ACTIVE"))
                .dormant(memberRepository.countByStatus("DORMANT"))
                .blacklist(memberRepository.countByStatus("BLACKLIST"))
                .withdrawn(memberRepository.countByStatus("WITHDRAWN"))
                .newToday(memberRepository.countByRegDateAfter(todayStart))
                .unverifiedEmail(memberRepository.countByEmailVerifiedFalse())
                .build();
    }

    private OrderStats buildOrderStats(LocalDateTime todayStart, Map<YearMonth, Long> monthlyRevenueMap) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        long currentMonthRevenue = monthlyRevenueMap.getOrDefault(currentMonth, 0L);
        long lastMonthRevenue = monthlyRevenueMap.getOrDefault(lastMonth, 0L);
        long changeAmount = currentMonthRevenue - lastMonthRevenue;
        Double changeRate = lastMonthRevenue == 0 ? null : (changeAmount * 100.0) / lastMonthRevenue;

        return OrderStats.builder()
                .paid(orderRepository.countByOrderStatus("PAID"))
                .shipping(orderRepository.countByOrderStatus("SHIPPING"))
                .delivered(orderRepository.countByOrderStatus("DELIVERED"))
                .todayCount(orderRepository.countByOrderStatusNotAndRegDateAfter("PENDING", todayStart))
                .totalRevenue(orderRepository.sumTotalRevenue())
                .todayRevenue(orderRepository.sumRevenueAfter(todayStart))
                .currentMonthRevenue(currentMonthRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .revenueChangeAmount(changeAmount)
                .revenueChangeRate(changeRate)
                .build();
    }

    private BoardStats buildBoardStats(LocalDateTime todayStart) {
        return BoardStats.builder()
                .total(boardRepository.count())
                .freeCount(boardRepository.countByBoardTypeAndDeletedFalse("FREE"))
                .reviewCount(boardRepository.countByBoardTypeAndDeletedFalse("REVIEW"))
                .todayCount(boardRepository.countByDeletedFalseAndRegDateAfter(todayStart))
                .build();
    }

    private ReservationStats buildReservationStats() {
        LocalDate today = LocalDate.now();

        return ReservationStats.builder()
                .total(reservationRepository.count())
                .pending(reservationRepository.countByStatus("대기"))
                .paidCount(reservationRepository.countByPayStatus("PAID"))
                .weddingThisWeek(reservationRepository.countByWeddingDateBetween(today, today.plusDays(7)))
                .build();
    }

    private InquiryStats buildInquiryStats() {
        return InquiryStats.builder()
                .openRooms(inquiryRoomRepository.countByStatus(InquiryRoomStatus.OPEN))
                .closedRooms(inquiryRoomRepository.countByStatus(InquiryRoomStatus.CLOSED))
                .build();
    }

    private ProductStats buildProductStats() {
        List<Product> lowStock = productRepository
                .findTop5ByDelFlagFalseAndStockQtyLessThanEqualOrderByStockQtyAsc(LOW_STOCK_THRESHOLD);

        List<LowStockProduct> lowStockProducts = lowStock.stream()
                .map(p -> LowStockProduct.builder()
                        .pno(p.getPno())
                        .pname(p.getPname())
                        .stockQty(p.getStockQty())
                        .build())
                .toList();

        return ProductStats.builder()
                .total(productRepository.countByDelFlagFalse())
                .lowStockCount(productRepository.countByDelFlagFalseAndStockQtyLessThanEqual(LOW_STOCK_THRESHOLD))
                .lowStockProducts(lowStockProducts)
                .build();
    }

    // "오늘의 할 일"은 관리자가 실제로 상태를 바꿔줘야 하는 항목만 넣음 (정보성 통계는 다른 패널에서 확인).
    // 예약 확정(manager-confirm)/문의 응대는 업체 매니저 화면에서 처리하는 영역이라 admin 할 일에서는 제외.
    // 0건이어도 항목 자체는 항상 보여줌 (몇 개나 있는지 목록으로 파악 가능하도록).
    private List<TodoItem> buildTodos(OrderStats orderStats, ProductStats productStats) {

        List<TodoItem> todos = new ArrayList<>();

        todos.add(TodoItem.builder()
                .label("결제 완료, 발송 대기 중인 주문")
                .count(orderStats.getPaid())
                .tone(orderStats.getPaid() > 0 ? "warning" : "info")
                .link("/admin/orders")
                .build());

        long pendingExchangeOrRefund = orderRepository.countPendingExchangeOrRefund();
        todos.add(TodoItem.builder()
                .label("환불/교환 요청 대기 중인 주문")
                .count(pendingExchangeOrRefund)
                .tone(pendingExchangeOrRefund > 0 ? "danger" : "info")
                .link("/admin/orders")
                .build());

        todos.add(TodoItem.builder()
                .label("재고 부족 상품 (답례품 등)")
                .count(productStats.getLowStockCount())
                .tone(productStats.getLowStockCount() > 0 ? "warning" : "info")
                .link("/admin/products")
                .build());

        long unansweredQuestions = qnaRepository.countUnansweredQuestions();
        todos.add(TodoItem.builder()
                .label("답변 안 된 상품 Q&A")
                .count(unansweredQuestions)
                .tone("info")
                .link("/admin/qna")
                .build());

        return todos;

    }
}
