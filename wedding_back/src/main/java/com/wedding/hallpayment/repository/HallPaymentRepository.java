package com.wedding.hallpayment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.hallpayment.domain.HallPayment;

public interface HallPaymentRepository extends JpaRepository<HallPayment, Long> {

    List<HallPayment> findByMemberEmailOrderByDueDateAsc(String memberEmail);

}
