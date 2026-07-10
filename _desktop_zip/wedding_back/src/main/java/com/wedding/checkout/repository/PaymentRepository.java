package com.wedding.checkout.repository;

import com.wedding.checkout.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 특정 주문의 결제 정보 조회
    @Query("select p from Payment p where p.orders.ono = :ono order by p.pmno desc")
    Optional<Payment> findLatestByOrderOno(@Param("ono") Long ono);

}
