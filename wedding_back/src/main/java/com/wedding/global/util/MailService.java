package com.wedding.global.util;

public interface MailService {

  void sendVerificationEmail(String toEmail, String token);

  void sendPasswordResetEmail(String toEmail, String token);

  // 주문 완료 확인 메일 (HYH 추가사항)
  void sendOrderConfirmationEmail(String toEmail, String orderNumber, int amount);

  // 주문 상태 변경(배송중/배송완료 등) 알림 메일
  void sendOrderStatusChangeEmail(String toEmail, String orderNumber, String newStatus);

}