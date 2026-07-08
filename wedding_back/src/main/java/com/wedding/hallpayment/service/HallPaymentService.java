package com.wedding.hallpayment.service;

import java.util.List;

import com.wedding.hallpayment.dto.HallPaymentDTO;

public interface HallPaymentService {

    Long register(HallPaymentDTO hallPaymentDTO);

    HallPaymentDTO get(Long paymentId);

    void modify(HallPaymentDTO hallPaymentDTO, String requesterEmail);

    void remove(Long paymentId, String requesterEmail);

    // 납부 기한 순으로 조회 (마이페이지 결제내역 / 준비관리 납부관리 공용)
    List<HallPaymentDTO> listByMember(String memberEmail);

}
