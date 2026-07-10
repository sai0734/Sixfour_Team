package com.wedding.member.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.global.util.CustomJWTException;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.member.domain.Member;
import com.wedding.member.exception.MemberBlockedException;
import com.wedding.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
public class AuthRefreshController {

    private final RedisTokenService redisTokenService;
    private final MemberRepository memberRepository;

    @RequestMapping("/api/auth/refresh")
    public Map<String, Object> refresh(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("refreshToken") String refreshToken
    ){

        if(refreshToken == null) {
            throw new CustomJWTException("NULL_REFRASH");
        }

        if(authHeader == null || authHeader.length() < 7) {
            throw new CustomJWTException("INVALID_STRING");
        }

        String accessToken = authHeader.substring(7);

        //Access 토큰이 만료되지 않았다면
        if(checkExpiredToken(accessToken) == false ) {
            return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
        }

        //Refresh토큰 검증
        Map<String, Object> claims = JWTUtil.validateToken(refreshToken);

        log.info("refresh ... claims: " + claims);

        String email = (String) claims.get("email");

        // 정지(BLACKLIST)/휴면(DORMANT) 회원은 accessToken 만료 시점에 재발급을 거부한다.
        // 관리자가 로그인 이후에 정지시켜도, 다음 accessToken 만료 타이밍(최대 24시간 이내)에
        // 여기서 확실히 걸러져 더 이상 갱신되지 않는다.
        Member member = memberRepository.findById(email).orElse(null);

        if (member != null) {

            // 정지 기간이 지났으면 자동으로 정상 복귀
            if ("BLACKLIST".equals(member.getStatus())
                    && member.getSuspendUntil() != null
                    && !member.getSuspendUntil().isAfter(LocalDateTime.now())) {
                member.reactivate();
                memberRepository.save(member);
            }

            if ("BLACKLIST".equals(member.getStatus())) {
                redisTokenService.deleteRefreshToken(email);
                throw new MemberBlockedException(
                        "ERROR_ACCOUNT_SUSPENDED",
                        member.getSuspendReason(),
                        member.getSuspendUntil() == null ? null : member.getSuspendUntil().toString());
            }

            if ("DORMANT".equals(member.getStatus())) {
                redisTokenService.deleteRefreshToken(email);
                throw new MemberBlockedException("ERROR_ACCOUNT_DORMANT", null, null);
            }

            if ("WITHDRAWN".equals(member.getStatus())) {
                redisTokenService.deleteRefreshToken(email);
                throw new MemberBlockedException("ERROR_ACCOUNT_WITHDRAWN", null, null);
            }
        }

        // Redis에 저장된 refresh 토큰과 일치하는지 확인 (로그아웃/강제만료 등으로 무효화된 토큰 차단)
        // 단, Redis 자체가 꺼져있는 환경(시연 PC 등)에서는 조회 결과가 항상 null이라
        // 이 검증을 건너뛰고 JWT 서명 검증만으로 통과시킨다 (로그인 유지 기능 자체가 죽지 않도록).
        String storedRefreshToken = redisTokenService.getRefreshToken(email);

        if (storedRefreshToken != null && !storedRefreshToken.equals(refreshToken)) {
            throw new CustomJWTException("INVALID_REFRESH");
        }

        String newAccessToken = JWTUtil.generateToken(claims, JWTUtil.ACCESS_TOKEN_MINUTES);

        // 원래 로그인할 때의 rememberMe 값을 그대로 이어받아서 재발급 (예전 발급된 토큰이라
        // claims에 rememberMe가 아예 없을 수도 있으니 그 경우는 false로 취급)
        boolean rememberMe = Boolean.TRUE.equals(claims.get("rememberMe"));
        int refreshMinutes = rememberMe ? JWTUtil.REFRESH_TOKEN_MINUTES_REMEMBER : JWTUtil.REFRESH_TOKEN_MINUTES_DEFAULT;

        String newRefreshToken = checkTime((Integer)claims.get("exp")) == true? JWTUtil.generateToken(claims, refreshMinutes) : refreshToken;

        // 토큰이 새로 발급됐다면 Redis 값도 갱신 (원래 rememberMe 값 그대로 TTL 재설정)
        if (!newRefreshToken.equals(refreshToken)) {
            redisTokenService.saveRefreshToken(email, newRefreshToken, rememberMe);
        }

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);

    }

    // 로그아웃: 현재 access 토큰을 블랙리스트에 등록하고, 저장된 refresh 토큰 삭제
    @PostMapping("/api/auth/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || authHeader.length() < 7) {
            return Map.of("result", "success");
        }

        String accessToken = authHeader.substring(7);

        try {
            Map<String, Object> claims = JWTUtil.validateToken(accessToken);

            String email = (String) claims.get("email");
            Integer exp = (Integer) claims.get("exp");

            long remainingSeconds = exp - (System.currentTimeMillis() / 1000);

            redisTokenService.addToBlacklist(accessToken, remainingSeconds);
            redisTokenService.deleteRefreshToken(email);

        } catch (Exception e) {
            log.info("logout: token already invalid, nothing to blacklist");
        }

        return Map.of("result", "success");
    }

    //시간이 1시간 미만으로 남았다면
    private boolean checkTime(Integer exp) {

        //JWT exp를 날짜로 변환
        java.util.Date expDate = new java.util.Date( (long)exp * (1000 ));

        //현재 시간과의 차이 계산 - 밀리세컨즈
        long gap   = expDate.getTime() - System.currentTimeMillis();

        //분단위 계산
        long leftMin = gap / (1000 * 60);

        //1시간도 안남았는지..
        return leftMin < 60;
    }

    private boolean checkExpiredToken(String token) {

        try{
            JWTUtil.validateToken(token);
        }catch(CustomJWTException ex) {
            if(ex.getMessage().equals("Expired")){
                return true;
            }
        }
        return false;
    }

}