package com.wedding.member.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.wedding.member.dto.SocialCompleteDTO;
import com.wedding.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/member")
public class MemberController {

  private final MemberService memberService;

  // 소셜(카카오) 로그인으로 최초 가입된 회원의 추가정보(이름/전화/약관동의) 입력 완료 처리
  // 이미 카카오 로그인 단계에서 JWT를 발급받은 상태이므로 이 경로는 로그인(JWT) 필수
  @PutMapping("/social-complete")
  public Map<String, String> completeSocialProfile(@Valid @RequestBody SocialCompleteDTO socialCompleteDTO) {

    log.info("social profile complete request: " + socialCompleteDTO);

    memberService.completeSocialProfile(socialCompleteDTO);

    return Map.of("result", "success");
  }

}