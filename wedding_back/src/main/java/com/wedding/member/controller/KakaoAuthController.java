package com.wedding.member.controller;
import org.springframework.web.bind.annotation.RestController;
import com.wedding.member.dto.KakaoAuthResultDTO;
import com.wedding.member.dto.KakaoLinkConfirmDTO;
import com.wedding.member.dto.KakaoSignupCompleteDTO;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.service.MemberService;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import jakarta.validation.Valid;
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

    // 카카오 인증 직후 호출됨. status에 따라 프론트가 다르게 처리:
    // READY -> 바로 로그인, CONFIRM_LINK -> 연동 확인창, PENDING_SIGNUP -> 추가정보 입력
    @GetMapping("/api/auth/kakao")
    public Map<String, Object> getMemberFromKakao(String accessToken) {

        log.info("accessToken ");
        log.info(accessToken);

        KakaoAuthResultDTO result = memberService.processKakaoAuth(accessToken);

        if ("READY".equals(result.status())) {
            MemberDTO memberDTO = result.memberDTO();
            Map<String, Object> claims = memberDTO.getClaims();
            String jwtAccessToken = JWTUtil.generateToken(claims, 60 * 24);
            String jwtRefreshToken = JWTUtil.generateToken(claims, 60*24*2);
            redisTokenService.saveRefreshToken(memberDTO.getEmail(), jwtRefreshToken, false);
            claims.put("accessToken", jwtAccessToken);
            claims.put("refreshToken", jwtRefreshToken);
            claims.put("profileComplete", true);
            claims.put("status", "READY");
            return claims;
        }

        if ("CONFIRM_LINK".equals(result.status())) {
            return Map.of(
                    "status", "CONFIRM_LINK",
                    "confirmToken", result.token(),
                    "kakaoEmail", result.kakaoEmail()
            );
        }

        // PENDING_SIGNUP
        return Map.of(
                "status", "PENDING_SIGNUP",
                "pendingToken", result.token(),
                "kakaoEmail", result.kakaoEmail()
        );
    }

    // CONFIRM_LINK 상태에서 사용자가 "연동할게요"를 선택했을 때 - 연동 처리 + 로그인(JWT 발급)까지 한번에
    @PutMapping("/api/auth/kakao/confirm-link")
    public Map<String, Object> confirmKakaoEmailLink(@Valid @RequestBody KakaoLinkConfirmDTO dto) {

        log.info("kakao email-match confirm request");

        return memberService.confirmKakaoEmailLink(dto);
    }

    // pendingToken + 추가정보로 카카오 가입을 최종 완료. 이 시점에 비로소 로그인 처리(JWT 발급)까지 됨.
    // 로그인 전 상태에서 호출하는 API라 /api/auth/ 하위에 둬서 JWTCheckFilter 인증 체크를 자동으로 건너뜀
    @PutMapping("/api/auth/kakao/complete")
    public Map<String, Object> completeKakaoSignup(@Valid @RequestBody KakaoSignupCompleteDTO dto) {

        log.info("kakao signup complete request");

        return memberService.completeKakaoSignup(dto);
    }

    @PutMapping("/api/auth/modify")
    public Map<String,String> modify(@RequestBody MemberModifyDTO memberModifyDTO) {
        log.info("member modify: " + memberModifyDTO);
        memberService.modifyMember(memberModifyDTO);
        return Map.of("result","modified");
    }
}