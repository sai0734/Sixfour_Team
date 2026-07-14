package com.wedding.member.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.member.dto.MemberDetailDTO;
import com.wedding.member.dto.SocialLinkDTO;
import com.wedding.member.dto.WithdrawDTO;
import com.wedding.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
public class MemberController {

  private final MemberService memberService;
  private final RedisTokenService redisTokenService;

  // 회원탈퇴 - status를 WITHDRAWN으로 전환하고 개인정보 익명화 + 현재 세션 즉시 로그아웃
  @DeleteMapping("/withdraw")
  public Map<String, String> withdraw(@Valid @RequestBody WithdrawDTO withdrawDTO,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    log.info("withdraw request: " + withdrawDTO.getEmail());

    memberService.withdrawMember(withdrawDTO.getEmail());

    // 탈퇴 즉시 현재 들고 있는 토큰도 강제 로그아웃 처리 (로그아웃 API와 동일한 로직 재사용)
    if (authHeader != null && authHeader.length() >= 7) {
      String accessToken = authHeader.substring(7);

      try {
        Map<String, Object> claims = JWTUtil.validateToken(accessToken);
        Integer exp = (Integer) claims.get("exp");
        long remainingSeconds = exp - (System.currentTimeMillis() / 1000);

        redisTokenService.addToBlacklist(accessToken, remainingSeconds);
        redisTokenService.deleteRefreshToken(withdrawDTO.getEmail());
      } catch (Exception e) {
        log.info("withdraw: token already invalid, nothing to blacklist");
      }
    }

    return Map.of("result", "success");
  }

  // 카카오 계정 연동 추가 (이미 일반가입한 계정에 카카오 로그인 수단을 추가로 연결)
  @PostMapping("/social-link")
  public Map<String, String> linkKakao(@Valid @RequestBody SocialLinkDTO socialLinkDTO) {

    log.info("kakao link request: " + socialLinkDTO.getEmail());

    memberService.linkKakaoAccount(socialLinkDTO.getEmail(), socialLinkDTO.getKakaoAccessToken());

    return Map.of("result", "success");
  }

  // 소셜 계정 연동 해제
  @DeleteMapping("/social-unlink/{provider}")
  public Map<String, String> unlinkSocial(@PathVariable String provider, @RequestParam String email) {

    log.info("social unlink request: " + email + " / " + provider);

    memberService.unlinkSocialAccount(email, provider);

    return Map.of("result", "success");
  }

  // 현재 연동되어 있는 소셜 provider 목록 조회 (마이페이지 표시용)
  @GetMapping("/social-accounts")
  public Map<String, List<String>> getSocialAccounts(@RequestParam String email) {

    return Map.of("providers", memberService.getLinkedProviders(email));
  }

  // 마이페이지 회원정보수정 - 이름/전화번호/생년월일/주소 조회
  @GetMapping("/detail")
  public MemberDetailDTO getDetail(@RequestParam String email) {
    return memberService.getMemberDetail(email);
  }

  // 마이페이지 회원정보수정 - 이름/전화번호/생년월일/주소 저장
  @PutMapping("/detail")
  public Map<String, String> modifyDetail(@Valid @RequestBody MemberDetailDTO memberDetailDTO) {
    memberService.modifyMemberDetail(memberDetailDTO);
    return Map.of("RESULT", "SUCCESS");
  }

}