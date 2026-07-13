package com.wedding.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.wedding.member.dto.JoinDTO;
import com.wedding.member.dto.PasswordResetConfirmDTO;
import com.wedding.member.dto.PasswordResetRequestDTO;
import com.wedding.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/auth")
public class JoinController {

  private final MemberService memberService;

  @PostMapping("/join")
  public Map<String, String> join(@Valid @RequestBody JoinDTO joinDTO) {

    log.info("join: " + joinDTO);

    memberService.join(joinDTO);

    return Map.of("result", "pending", "message", "인증 메일을 발송했습니다. 메일함을 확인해 주세요.");
  }

  @GetMapping("/check-email")
  public Map<String, Boolean> checkEmail(@RequestParam String email) {

    boolean available = memberService.checkEmailAvailable(email);

    return Map.of("available", available);
  }

  @GetMapping("/check-nickname")
  public Map<String, Boolean> checkNickname(@RequestParam String nickname) {

    boolean available = memberService.checkNicknameAvailable(nickname);

    return Map.of("available", available);
  }

  @GetMapping("/check-phone")
  public Map<String, Boolean> checkPhone(@RequestParam String phone) {

    String status = memberService.getPhoneCheckStatus(phone);

    return Map.of(
            "available", "AVAILABLE".equals(status),
            "blocked", "BLOCKED".equals(status)
    );
  }

  // 이메일 인증 링크 클릭 시 진입하는 경로 (사용자가 메일 클라이언트에서 직접 클릭)
  @GetMapping("/verify-email")
  public ResponseEntity<String> verifyEmail(@RequestParam String token) {

    String result = memberService.verifyEmail(token);

    String title;
    String message;

    switch (result) {
      case "SUCCESS":
        title = "인증 완료";
        message = "이메일 인증이 완료되었습니다. 이제 로그인하실 수 있어요.";
        break;
      case "ALREADY_VERIFIED":
        title = "이미 인증됨";
        message = "이미 인증이 완료된 계정입니다.";
        break;
      case "EXPIRED":
        title = "인증 만료";
        message = "인증 링크가 만료되었습니다. 다시 시도해 주세요.";
        break;
      case "PHONE_DUPLICATE":
        title = "가입 실패";
        message = "이미 사용 중인 휴대폰 번호입니다. 다른 번호로 다시 가입해 주세요.";
        break;
      case "PHONE_BLOCKED":
        title = "가입 실패";
        message = "정지 또는 휴면 처리된 회원과 동일한 휴대폰 번호입니다. 관리자에게 문의해주세요.";
        break;
      default:
        title = "인증 실패";
        message = "유효하지 않은 인증 링크입니다.";
    }

    String html =
            "<html><head><meta charset='UTF-8'></head>"
                    + "<body style='font-family:sans-serif; text-align:center; padding-top:80px;'>"
                    + "<h2>" + title + "</h2>"
                    + "<p>" + message + "</p>"
                    + "</body></html>";

    return ResponseEntity.ok()
            .headers(new HttpHeaders() {{ setContentType(MediaType.TEXT_HTML); }})
            .body(html);
  }

  @PostMapping("/password-reset/request")
  public Map<String, String> requestPasswordReset(@RequestBody PasswordResetRequestDTO requestDTO) {

    log.info("password reset request: " + requestDTO.getEmail());

    memberService.requestPasswordReset(requestDTO);

    // 이메일 존재 여부와 무관하게 항상 같은 메시지 (계정 존재 여부 유추 방지)
    return Map.of("result", "pending", "message", "가입된 이메일이라면 비밀번호 재설정 메일을 발송했습니다.");
  }

  @PostMapping("/password-reset/confirm")
  public Map<String, String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO confirmDTO) {

    log.info("password reset confirm: token=" + confirmDTO.getToken());

    String result = memberService.confirmPasswordReset(confirmDTO);

    if ("SUCCESS".equals(result)) {
      return Map.of("result", "success", "message", "비밀번호가 재설정되었습니다.");
    }

    return Map.of("result", "fail", "reason", result);
  }

  @PostMapping("/resend-verification")
  public Map<String, String> resendVerification(@RequestBody JoinDTO joinDTO) {

    log.info("resend verification: " + joinDTO.getEmail());

    String result = memberService.resendVerification(joinDTO.getEmail());

    if ("SUCCESS".equals(result)) {
      return Map.of("result", "success", "message", "인증 메일을 다시 발송했습니다.");
    }

    return Map.of("result", "fail", "reason", result);
  }

}