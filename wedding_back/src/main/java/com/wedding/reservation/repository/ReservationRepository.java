package com.wedding.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberEmailOrderByReservationIdDesc(String memberEmail);

    // 재원 추가 - 업체 상세페이지 "결제 횟수" 표시용 (인기 업체 패키지 구성 참고 지표)
    long countByCmnoAndPayStatus(Long cmno, String payStatus);

    // 승진 추가 - 업체 예약관리 목록
    List<Reservation> findByCmnoOrderByReservationIdDesc(Long cmno);

}
