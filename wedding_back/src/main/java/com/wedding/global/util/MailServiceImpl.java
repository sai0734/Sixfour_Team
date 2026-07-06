package com.wedding.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class MailServiceImpl implements MailService {

  private final JavaMailSender mailSender;

  @Value("${com.wedding.server.host}")
  private String serverHost;

  @Value("${com.wedding.front.host}")
  private String frontHost;

  private static final Map<String, String> STATUS_LABEL = Map.of(
          "PAID", "결제완료",
          "SHIPPING_READY", "배송준비중",
          "SHIPPING", "배송중",
          "DELIVERED", "배송완료",
          "REFUNDED", "환불완료",
          "CANCELLED", "주문취소"
  );

  @Override
  public void sendVerificationEmail(String toEmail, String token) {

    String verifyLink = serverHost + "/api/auth/verify-email?token=" + token;

    String subject = "[Wedding] 이메일 인증을 완료해 주세요";

    String content =
        "<div style='font-family:sans-serif; line-height:1.6;'>"
      + "<h2>이메일 인증</h2>"
      + "<p>아래 버튼을 눌러 이메일 인증을 완료해 주세요. (30분 내 유효)</p>"
      + "<a href='" + verifyLink + "' "
      + "style='display:inline-block; padding:12px 24px; background-color:#3b82f6; "
      + "color:#ffffff; text-decoration:none; border-radius:6px;'>이메일 인증하기</a>"
      + "<p style='margin-top:16px; color:#888;'>버튼이 안 보이면 아래 링크를 복사해 브라우저에 붙여넣어 주세요.<br/>"
      + verifyLink + "</p>"
      + "</div>";

    sendHtmlMail(toEmail, subject, content);
  }

  @Override
  public void sendPasswordResetEmail(String toEmail, String token) {

    // 프론트 재설정 페이지로 연결 (해당 페이지에서 token을 받아 새 비밀번호와 함께
    // POST /api/auth/password-reset/confirm 호출하는 형태로 추후 프론트 구현 필요)
    String resetLink = frontHost + "/auth/reset-password?token=" + token;

    String subject = "[Wedding] 비밀번호 재설정 안내";

    String content =
        "<div style='font-family:sans-serif; line-height:1.6;'>"
      + "<h2>비밀번호 재설정</h2>"
      + "<p>아래 버튼을 눌러 비밀번호를 재설정해 주세요. (30분 내 유효)</p>"
      + "<a href='" + resetLink + "' "
      + "style='display:inline-block; padding:12px 24px; background-color:#3b82f6; "
      + "color:#ffffff; text-decoration:none; border-radius:6px;'>비밀번호 재설정하기</a>"
      + "<p style='margin-top:16px; color:#888;'>본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.<br/>"
      + resetLink + "</p>"
      + "</div>";

    sendHtmlMail(toEmail, subject, content);
  }

  private void sendHtmlMail(String toEmail, String subject, String content) {

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(content, true);

      mailSender.send(message);

      log.info("mail sent to: " + toEmail);

    } catch (MessagingException e) {
      log.error("Failed to send mail to " + toEmail, e);
      throw new RuntimeException("이메일 발송에 실패했습니다.", e);
    }
  }

  // 주문 완료 확인 메일 (HYH 추가)
  @Override
  public void sendOrderConfirmationEmail(String toEmail, String orderNumber, int amount) {

    String subject = "[Wedding] 주문이 완료되었습니다 (주문번호 " + orderNumber + ")";

    String content =
            "<div style='font-family:sans-serif; line-height:1.6;'>"
                    + "<h2>주문 완료 안내</h2>"
                    + "<p>주문이 정상적으로 완료되었습니다.</p>"
                    + "<p><b>주문번호:</b> " + orderNumber + "</p>"
                    + "<p><b>결제금액:</b> " + String.format("%,d", amount) + "원</p>"
                    + "<p style='margin-top:16px; color:#888;'>주문해 주셔서 감사합니다.</p>"
                    + "</div>";

    sendHtmlMail(toEmail, subject, content);
  }

  // 주문 상태 변경 알림 메일
  @Override
  public void sendOrderStatusChangeEmail(String toEmail, String orderNumber, String newStatus) {

    String label = STATUS_LABEL.getOrDefault(newStatus, newStatus);

    String subject = "[Wedding] 주문 상태가 변경되었습니다 (주문번호 " + orderNumber + ")";
    String content =
            "<div style='font-family:sans-serif; line-height:1.6;'>"
                    + "<h2>주문 상태 변경 안내</h2>"
                    + "<p><b>주문번호:</b> " + orderNumber + "</p>"
                    + "<p><b>변경된 상태:</b> " + label + "</p>"
                    + "<p style='margin-top:16px; color:#888;'>문의사항이 있으시면 고객센터로 연락해 주세요.</p>"
                    + "</div>";

    sendHtmlMail(toEmail, subject, content);
  }

}