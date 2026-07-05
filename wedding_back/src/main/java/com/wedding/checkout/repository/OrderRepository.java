package com.wedding.checkout.repository;

import com.wedding.checkout.domain.Orders;
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

}