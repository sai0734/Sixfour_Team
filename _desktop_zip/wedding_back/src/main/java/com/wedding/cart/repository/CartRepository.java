package com.wedding.cart.repository;

import com.wedding.cart.domain.CartOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartOrder, Long> {

    // 회원의 장바구니 조회
    @Query("select co from CartOrder co where co.member.email = :email")
    Optional<CartOrder> getCartOfMember(@Param("email") String email);

}