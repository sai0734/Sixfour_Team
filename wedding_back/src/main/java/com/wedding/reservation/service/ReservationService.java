package com.wedding.reservation.service;

import java.util.List;

// 승진 코드 추가
import com.wedding.reservation.dto.ReservationBulkPaymentConfirmRequestDTO;
import com.wedding.reservation.dto.ReservationBulkPaymentPrepareDTO;
// 승진 코드 추가 끝
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

    // 업체 상세페이지 "결제 횟수" 표시용 - 결제 완료(PAID) 건만 카운트
    long getPaymentCount(Long cmno);

    // 예약 날짜 선택 시 미리 확인 - 같은 업체+같은 옵션+같은 날짜 예약이 이미 있는지
    boolean isDateTaken(Long cmno, String optionName, java.time.LocalDate weddingDate);
    // ↑↑↑ 재원 추가

    // 승진 코드 추가
    // 묶음 결제 - 주문번호 발급 (여러 예약 총액 계산)
    ReservationBulkPaymentPrepareDTO prepareBulkPayment(List<Long> reservationIds, String memberEmail);

    // 묶음 결제 - 토스 승인 후 전체 PAID 처리
    List<ReservationDTO> confirmBulkPayment(String memberEmail, ReservationBulkPaymentConfirmRequestDTO requestDTO);

    // 묶음 결제 취소/실패 처리
    void cancelBulkPayment(List<Long> reservationIds, String memberEmail);
    // 승진 코드 추가 끝

    // 승진 코드 추가 - 업체 예약관리
    List<ReservationDTO> listByCompany(Long cmno, String callerEmail);

    List<ReservationDTO> listMyManagedCompany(String callerEmail);

    ReservationDTO confirmByManager(Long reservationId, String callerEmail);
    // 승진 코드 추가 끝

}
