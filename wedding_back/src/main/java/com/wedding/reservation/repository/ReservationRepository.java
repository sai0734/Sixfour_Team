package com.wedding.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberEmailOrderByReservationIdDesc(String memberEmail);

    // 승진 코드 추가 - 업체 예약관리 목록
    List<Reservation> findByCmnoOrderByReservationIdDesc(Long cmno);
    // 승진 코드 추가 끝

}
