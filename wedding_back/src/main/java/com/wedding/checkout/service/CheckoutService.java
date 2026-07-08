package com.wedding.checkout.service;

import com.wedding.checkout.dto.AddressDTO;
import com.wedding.checkout.dto.CheckoutRequestDTO;
import com.wedding.checkout.dto.ConfirmRequestDTO;
import com.wedding.checkout.dto.OrderDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface CheckoutService {

    // 주문 생성 (결제 전, PENDING 상태)
    OrderDTO prepareOrder(String memberEmail, CheckoutRequestDTO requestDTO);

    // 결제 승인 처리 (토스 승인 API 호출 + 주문상태 변경 + 장바구니 정리 + 알림 발송)
    OrderDTO confirmPayment(String memberEmail, ConfirmRequestDTO confirmRequestDTO);

    // 결제 취소/실패 시 주문 취소 처리
    void cancelOrder(String memberEmail, String orderNumber);

    // 가장 최근 결제완료 주문의 배송지 조회 (배송지 불러오기용)
    AddressDTO getLatestAddress(String memberEmail);

    // 회원 본인 주문 목록
    List<OrderDTO> listMyOrders(String memberEmail);

}