package com.wedding.checkout.repository;

import com.wedding.checkout.domain.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    // 주문번호로 주문 조회 (본인 확인 포함)
    @Query("select o from Orders o where o.orderNumber = :orderNumber and o.member.email = :memberEmail")
    Optional<Orders> findByOrderNumberAndMember(@Param("orderNumber") String orderNumber, @Param("memberEmail") String memberEmail);

    // 회원의 주문 목록 조회 (최신순, PENDING상태 제외)
    @Query("select o from Orders o where o.member.email = :memberEmail and o.orderStatus != 'PENDING' order by o.ono desc")
    List<Orders> listByMember(@Param("memberEmail") String memberEmail);

    // 회원의 가장 최근 결제완료 주문 1건 조회 (배송지 불러오기용)
    @Query("select o from Orders o where o.member.email = :memberEmail and o.orderStatus = 'PAID' order by o.ono desc limit 1")
    Optional<Orders> findLatestPaidOrderByMember(@Param("memberEmail") String memberEmail);

    // 관리자용 주문 리스트 조회 (주문번호/주문자명 검색 + 상태 필터)
    @Query("select o from Orders o where o.orderStatus != 'PENDING' " +
            "and (:keyword is null or :keyword = '' " +
            "     or o.orderNumber like concat('%', :keyword, '%') " +
            "     or o.receiverName like concat('%', :keyword, '%')) " +
            "and (:status is null or :status = '' or o.orderStatus = :status)")
    Page<Orders> adminSearchOrders(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   Pageable pageable);

    // 관리자 대시보드용 집계 (PENDING 제외 - 결제 미완료 임시주문은 실적/매출에서 제외)
    @Query("select count(o) from Orders o where o.orderStatus = :status and o.orderStatus != 'PENDING'")
    long countByOrderStatus(@Param("status") String status);

    @Query("select coalesce(sum(o.totalPrice), 0) from Orders o " +
            "where o.orderStatus not in ('PENDING', 'REFUNDED', 'CANCELLED')")
    long sumTotalRevenue();

    @Query("select coalesce(sum(o.totalPrice), 0) from Orders o " +
            "where o.orderStatus not in ('PENDING', 'REFUNDED', 'CANCELLED') and o.regDate >= :dateTime")
    long sumRevenueAfter(@Param("dateTime") java.time.LocalDateTime dateTime);

    long countByOrderStatusNotAndRegDateAfter(String excludedStatus, java.time.LocalDateTime dateTime);

    // 월별 매출 추이 + 전월대비 비교 계산용 원본 데이터 (날짜, 금액). 집계는 서비스단에서 월별로 묶음.
    @Query("select o.regDate, o.totalPrice from Orders o " +
            "where o.orderStatus not in ('PENDING', 'REFUNDED', 'CANCELLED') and o.regDate >= :since")
    List<Object[]> findRevenueRowsSince(@Param("since") java.time.LocalDateTime since);

}