package com.wedding.admin.dashboard.service;

import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.BoardStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.CategoryRevenueCard;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.InquiryStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.LowStockProduct;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.MemberStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.MonthlyRevenuePoint;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.OrderStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.CompanyRankingItem;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.ProductStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.ReservationStats;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.TodoBreakdownItem;
import com.wedding.admin.dashboard.dto.AdminDashboardSummaryDTO.TodoItem;
import com.wedding.board.repository.BoardRepository;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.company.domain.CompanyCategory;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private static final Map<CompanyCategory, String> CATEGORY_LABELS = Map.of(
            CompanyCategory.HALL, "웨딩홀",
            CompanyCategory.DRESS, "드레스",
            CompanyCategory.STUDIO, "스튜디오",
            CompanyCategory.MAKEUP, "메이크업"
    );

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
        List<CategoryRevenueCard> categoryRevenueCards = buildCategoryRevenueCards();
        List<CompanyRankingItem> topCompaniesOverall = buildTopCompaniesOverall();

        List<TodoItem> todos = buildTodos(orderStats, productStats);

        return AdminDashboardSummaryDTO.builder()
                .memberStats(memberStats)
                .orderStats(orderStats)
                .boardStats(boardStats)
                .reservationStats(reservationStats)
                .inquiryStats(inquiryStats)
                .productStats(productStats)
                .categoryRevenueCards(categoryRevenueCards)
                .topCompaniesOverall(topCompaniesOverall)
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

    // 업체 현황 카드 4개 - 웨딩홀/드레스/스튜디오/메이크업 순서로, 카테고리별 결제완료 예약 금액 합산 기준
    // 매출 1위/평균/꼴찌 업체를 계산 (평균 = 카테고리 내 매출 있는 업체 총합 / 업체 수)
    private List<CategoryRevenueCard> buildCategoryRevenueCards() {
        List<CompanyCategory> orderedCategories = List.of(
                CompanyCategory.HALL, CompanyCategory.DRESS, CompanyCategory.STUDIO, CompanyCategory.MAKEUP);

        return orderedCategories.stream()
                .map(this::buildCategoryRevenueCard)
                .toList();
    }

    private CategoryRevenueCard buildCategoryRevenueCard(CompanyCategory category) {
        Pageable top1 = PageRequest.of(0, 1);

        List<Object[]> topRows = reservationRepository.sumAmountByCompanyInCategoryDesc(category, top1);
        List<Object[]> bottomRows = reservationRepository.sumAmountByCompanyInCategoryAsc(category, top1);

        long totalAmount = reservationRepository.sumAmountInCategory(category);
        long companyCount = reservationRepository.countDistinctCompaniesInCategoryWithPaidReservation(category);
        long averageAmount = companyCount == 0 ? 0 : totalAmount / companyCount;

        return CategoryRevenueCard.builder()
                .category(category.name())
                .categoryLabel(CATEGORY_LABELS.get(category))
                .topCompanyName(topRows.isEmpty() ? "-" : (String) topRows.get(0)[0])
                .topAmount(topRows.isEmpty() ? 0 : ((Number) topRows.get(0)[1]).longValue())
                .averageAmount(averageAmount)
                .bottomCompanyName(bottomRows.isEmpty() ? "-" : (String) bottomRows.get(0)[0])
                .bottomAmount(bottomRows.isEmpty() ? 0 : ((Number) bottomRows.get(0)[1]).longValue())
                .build();
    }

    // 업체 매출 전체 순위 - 4개 카테고리(웨딩홀/드레스/스튜디오/메이크업) 구분 없이 결제완료 예약 금액
    // 합산 기준 TOP 10
    private List<CompanyRankingItem> buildTopCompaniesOverall() {
        List<Object[]> rows = reservationRepository.sumAmountByCompanyOverallDesc(PageRequest.of(0, 10));

        List<CompanyRankingItem> ranking = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            String companyName = (String) row[0];
            CompanyCategory category = (CompanyCategory) row[1];
            long amount = ((Number) row[2]).longValue();

            ranking.add(CompanyRankingItem.builder()
                    .rank(rank++)
                    .companyName(companyName)
                    .category(category.name())
                    .categoryLabel(CATEGORY_LABELS.get(category))
                    .amount(amount)
                    .build());
        }

        return ranking;
    }

    // "오늘의 할 일"은 관리자가 실제로 상태를 바꿔줘야 하는 항목만 넣음 (정보성 통계는 다른 패널에서 확인).
    // 예약 확정(manager-confirm)/문의 응대는 업체 매니저 화면에서 처리하는 영역이라 admin 할 일에서는 제외.
    // 0건이어도 항목 자체는 항상 보여줌 (몇 개나 있는지 목록으로 파악 가능하도록).
    private List<TodoItem> buildTodos(OrderStats orderStats, ProductStats productStats) {

        List<TodoItem> todos = new ArrayList<>();

        // 결제완료/배송준비/교환신청/환불신청 대기 주문을 카드 하나로 묶음 (모두 /admin/orders에서 처리)
        long paidCount = orderStats.getPaid();
        long shippingReadyCount = orderRepository.countByOrderStatus("SHIPPING_READY");
        long exchangeRequestedCount = orderRepository.countByOrderStatus("EXCHANGE_REQUESTED");
        long refundRequestedCount = orderRepository.countByOrderStatus("REFUND_REQUESTED");
        long orderActionTotal = paidCount + shippingReadyCount + exchangeRequestedCount + refundRequestedCount;
        todos.add(TodoItem.builder()
                .label("처리 필요 주문")
                .count(orderActionTotal)
                .tone(orderActionTotal > 0 ? "danger" : "info")
                .link("/admin/orders")
                .breakdown(List.of(
                        TodoBreakdownItem.builder().label("결제완료").count(paidCount).build(),
                        TodoBreakdownItem.builder().label("배송준비").count(shippingReadyCount).build(),
                        TodoBreakdownItem.builder().label("교환신청").count(exchangeRequestedCount).build(),
                        TodoBreakdownItem.builder().label("환불신청").count(refundRequestedCount).build()
                ))
                .build());

        todos.add(TodoItem.builder()
                .label("재고 부족 상품 (답례품 등)")
                .count(productStats.getLowStockCount())
                .tone(productStats.getLowStockCount() > 0 ? "danger" : "info")
                .link("/admin/products")
                .build());

        long unansweredQuestions = qnaRepository.countUnansweredQuestions();
        todos.add(TodoItem.builder()
                .label("답변 안 된 상품 Q&A")
                .count(unansweredQuestions)
                .tone(unansweredQuestions > 0 ? "danger" : "info")
                .link("/admin/qna")
                .build());

        return todos;

    }
}
