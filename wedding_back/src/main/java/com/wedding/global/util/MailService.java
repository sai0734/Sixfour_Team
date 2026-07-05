package com.wedding.global.util;

public interface MailService {

  void sendVerificationEmail(String toEmail, String token);

  void sendPasswordResetEmail(String toEmail, String token);

  // 주문 완료 확인 메일 (HYH 추가사항)
  void sendOrderConfirmationEmail(String toEmail, String orderNumber, int amount);

}