package com.wedding.member.controller;

import org.springframework.web.bind.annotation.RestController;

import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.service.MemberService;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@Log4j2
@RequiredArgsConstructor
public class KakaoAuthController {
    
    private final MemberService memberService;
    private final RedisTokenService redisTokenService;

    @GetMapping("/api/auth/kakao")
    public Map<String, Object> getMemberFromKakao(String accessToken) {
        
        log.info("accessToken ");
        log.info(accessToken);

        MemberDTO memberDTO = memberService.getKakaoMember(accessToken);

        Map<String, Object> claims = memberDTO.getClaims();

        String jwtAccessToken = JWTUtil.generateToken(claims, 10);
        String jwtRefreshToken = JWTUtil.generateToken(claims, 60*1);

        redisTokenService.saveRefreshToken(memberDTO.getEmail(), jwtRefreshToken, false);

        claims.put("accessToken", jwtAccessToken);
        claims.put("refreshToken", jwtRefreshToken);
        claims.put("profileComplete", memberService.hasProfile(memberDTO.getEmail()));

        return claims;
    }
      @PutMapping("/api/auth/modify")
  public Map<String,String> modify(@RequestBody MemberModifyDTO memberModifyDTO) {

    log.info("member modify: " + memberModifyDTO);

    memberService.modifyMember(memberModifyDTO);

    return Map.of("result","modified");

  }
}