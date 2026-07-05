package com.wedding.checkout.repository;

import com.wedding.checkout.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 이 회원이 이 상품을 구매했고, 아직 리뷰 안 남긴 주문내역 조회 (리뷰 작성 자격+대상 확보)
    @Query("select oi from OrderItem oi " +
            "where oi.orders.member.email = :email and oi.product.pno = :pno " +
            "and not exists (select 1 from Review r where r.orderItem = oi)")
    Optional<OrderItem> findAvailableOrderItem(@Param("email") String email, @Param("pno") Long pno);
}