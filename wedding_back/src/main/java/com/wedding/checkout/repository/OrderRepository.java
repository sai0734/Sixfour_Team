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

    // 회원의 주문 목록 조회 (최신순)
    @Query("select o from Orders o where o.member.email = :memberEmail order by o.ono desc")
    List<Orders> listByMember(@Param("memberEmail") String memberEmail);

    // 회원의 가장 최근 결제완료 주문 1건 조회 (배송지 불러오기용)
    @Query("select o from Orders o where o.member.email = :memberEmail and o.orderStatus = 'PAID' order by o.ono desc limit 1")
    Optional<Orders> findLatestPaidOrderByMember(@Param("memberEmail") String memberEmail);

    // 신규 추가: 관리자용 주문 리스트 조회 (주문번호/주문자명 검색 + 상태 필터)
    @Query("select o from Orders o where o.orderStatus != 'PENDING' " +
            "and (:keyword is null or :keyword = '' " +
            "     or o.orderNumber like concat('%', :keyword, '%') " +
            "     or o.receiverName like concat('%', :keyword, '%')) " +
            "and (:status is null or :status = '' or o.orderStatus = :status)")
    Page<Orders> adminSearchOrders(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   Pageable pageable);

}