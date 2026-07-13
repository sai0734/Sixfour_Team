package com.wedding.reservation.dto;

import lombok.Data;

// 재원 추가 - 업체 예약 결제 승인 요청 (프론트 결제완료 페이지 → 서버)
@Data
public class ReservationPaymentConfirmRequestDTO {

    private String paymentKey;

    private String orderNumber;

    private int amount;

}
