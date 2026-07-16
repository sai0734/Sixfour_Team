package com.wedding.reservation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberEmailOrderByReservationIdDesc(String memberEmail);

    // 재원 추가 - 업체 상세페이지 "결제 횟수" 표시용 (인기 업체 패키지 구성 참고 지표)
    long countByCmnoAndPayStatus(Long cmno, String payStatus);

    // 재원 추가 - 같은 업체+같은 옵션+같은 예식일 예약이 이미 있는지 확인 (중복 예약 방지)
    boolean existsByCmnoAndOptionNameAndWeddingDate(
            Long cmno, String optionName, LocalDate weddingDate);

    // 승진 추가 - 업체 예약관리 목록
    List<Reservation> findByCmnoOrderByReservationIdDesc(Long cmno);

    // 관리자 대시보드용 집계
    long countByStatus(String status);

    long countByPayStatus(String payStatus);

    long countByWeddingDateBetween(LocalDate start, LocalDate end);

}
