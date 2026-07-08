package com.wedding.checkout.controller;

import com.wedding.checkout.dto.AddressDTO;
import com.wedding.checkout.dto.CheckoutRequestDTO;
import com.wedding.checkout.dto.ConfirmRequestDTO;
import com.wedding.checkout.dto.OrderDTO;
import com.wedding.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    // 주문 생성 (결제 전)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/prepare")
    public OrderDTO prepare(Principal principal, @RequestBody CheckoutRequestDTO requestDTO) {

        log.info("CheckoutController_prepare 실행~~~~~~~~");

        return checkoutService.prepareOrder(principal.getName(), requestDTO);
    }

    // 결제 승인
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

    // 최근 배송지 불러오기
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/last-address")
    public AddressDTO lastAddress(Principal principal) {

        log.info("CheckoutController_lastAddress 실행~~~~~~~~");

        return checkoutService.getLatestAddress(principal.getName());
    }

    // 회원 본인 주문 목록(마이페이지 결제 내역용)
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/api/my-order")
    public List<OrderDTO> myOrders(Principal principal) {

        log.info("CheckoutController_myOrders 실행~~~~~~~~");

        return checkoutService.listMyOrders(principal.getName());

    }

}