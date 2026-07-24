package com.wedding.checkout.service;

import com.wedding.checkout.domain.Orders;

public interface OrderNotificationService {

    // 비동기로 주문 완료 이메일 발송 (실패해도 주문 자체는 이미 완료된 상태로 유지됨)
    void sendOrderConfirmation(String memberEmail, Orders orders);

    // 상태 변경(배송중/배송완료/환불 등) 알림 이메일 (비동기)
    void sendStatusChangeNotification(String memberEmail, Orders orders, String newStatus);

}
