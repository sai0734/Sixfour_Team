package com.wedding.checkout.service;

import com.wedding.checkout.domain.Orders;
import com.wedding.global.util.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderNotificationService {

    private final MailService mailService;

    // 비동기로 주문 완료 이메일 발송 (실패해도 주문 자체는 이미 완료된 상태로 유지됨)
    @Async
    public void sendOrderConfirmation(String memberEmail, Orders orders) {

        try {
            mailService.sendOrderConfirmationEmail(memberEmail, orders.getOrderNumber(), orders.getTotalPrice());
        } catch (Exception e) {
            log.error("주문 확인 이메일 발송 실패", e);
        }

    }

    // 상태 변경(배송중/배송완료/환불 등) 알림 이메일 (비동기)
    @Async
    public void sendStatusChangeNotification(String memberEmail, Orders orders, String newStatus) {
        try {
            mailService.sendOrderStatusChangeEmail(memberEmail, orders.getOrderNumber(), newStatus);
        } catch (Exception e) {
            log.error("주문 상태변경 이메일 발송 실패", e);
        }
    }

}