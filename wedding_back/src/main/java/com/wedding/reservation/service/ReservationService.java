package com.wedding.reservation.service;

import java.util.List;

import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.dto.ReservationPaymentConfirmRequestDTO;

public interface ReservationService {

    Long register(ReservationDTO reservationDTO);

    ReservationDTO get(Long reservationId);

    void modify(ReservationDTO reservationDTO);

    void remove(Long reservationId);

    List<ReservationDTO> listByMember(String memberEmail);

    // ↓↓↓ 재원 추가 - 업체 예약 결제 (날짜/옵션 선택 → 토스 결제)
    // 주문번호 발급 (결제창 열기 전)
    ReservationDTO preparePayment(Long reservationId, String memberEmail);

    // 결제 승인 (토스 서버 확인 + 상태 반영)
    ReservationDTO confirmPayment(Long reservationId, String memberEmail, ReservationPaymentConfirmRequestDTO requestDTO);

    // 결제 취소/실패 처리
    void cancelPayment(Long reservationId, String memberEmail);
    // ↑↑↑ 재원 추가

}
