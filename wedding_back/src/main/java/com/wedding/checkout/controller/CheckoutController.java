package com.wedding.checkout.controller;

import com.wedding.checkout.dto.*;
import com.wedding.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    // 주문 생성 (결제 전, PENDING 상태)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/prepare")
    public OrderDTO prepare(Principal principal, @RequestBody CheckoutRequestDTO requestDTO) {

        log.info("CheckoutController_prepare 실행~~~~~~~~");

        return checkoutService.prepareOrder(principal.getName(), requestDTO);
    }

    // 결제 승인 처리 (토스 승인 API 호출 + 주문상태 변경 + 장바구니 정리 + 알림 발송)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/confirm")
    public OrderDTO confirm(Principal principal, @RequestBody ConfirmRequestDTO confirmRequestDTO) {

        log.info("CheckoutController_confirm 실행~~~~~~~~");

        return checkoutService.confirmPayment(principal.getName(), confirmRequestDTO);
    }

    // 결제 취소/실패 처리
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/cancel/{orderNumber}")
    public void cancel(Principal principal, @PathVariable String orderNumber) {

        log.info("CheckoutController_cancel 실행~~~~~~~~");

        checkoutService.cancelOrder(principal.getName(), orderNumber);
    }

    // 가장 최근 결제완료 주문의 배송지 조회 (배송지 불러오기용)
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/last-address")
    public AddressDTO lastAddress(Principal principal) {

        log.info("CheckoutController_lastAddress 실행~~~~~~~~");

        return checkoutService.getLatestAddress(principal.getName());
    }

    // 회원 본인 주문 목록(마이페이지 결제 내역용)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my-order")
    public List<OrderDTO> myOrders(Principal principal) {

        log.info("CheckoutController_myOrders 실행~~~~~~~~");

        return checkoutService.listMyOrders(principal.getName());

    }

    // 교환/환불 신청 (사유 저장만 - 배송완료 주문만 신청 가능)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/orders/{orderNumber}/exchange-return")
    public Map<String, String> requestExchangeReturn(Principal principal, @PathVariable String orderNumber,
                                                     @RequestBody ExchangeReturnRequestDTO requestDTO) {

        log.info("CheckoutController_requestExchangeReturn 실행~~~~~~~~");

        checkoutService.requestExchangeReturn(principal.getName(), orderNumber, requestDTO);

        return Map.of("RESULT", "SUCCESS");

    }

}