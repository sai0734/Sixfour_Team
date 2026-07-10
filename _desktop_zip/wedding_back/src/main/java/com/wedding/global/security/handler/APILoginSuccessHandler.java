package com.wedding.global.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.google.gson.Gson;
import com.wedding.member.dto.MemberDTO;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.member.repository.LoginFailRepository;
import com.wedding.member.repository.MemberRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class APILoginSuccessHandler implements AuthenticationSuccessHandler{

    private final LoginFailRepository loginFailRepository;
    private final RedisTokenService redisTokenService;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException{
        log.info("-------------------------------");
        log.info(authentication);
        log.info("-------------------------------");

        MemberDTO memberDTO = (MemberDTO)authentication.getPrincipal();

        // 로그인 성공 시 실패 카운트/잠금 초기화
        loginFailRepository.getByMemberEmail(memberDTO.getEmail())
                .ifPresent(loginFail -> {
                    loginFail.reset();
                    loginFailRepository.save(loginFail);
                });

        // 관리자 회원관리 - 최근 로그인 시각 갱신
        memberRepository.findById(memberDTO.getEmail())
                .ifPresent(member -> {
                    member.touchLogin();
                    memberRepository.save(member);
                });

        Map<String, Object> claims  = memberDTO.getClaims();

        // 로그인 유지("rememberMe") 체크 여부 - 30일이면 refreshToken 자체도 30일짜리로,
        // 아니면 7일짜리로 발급 (Redis TTL이랑 반드시 같은 기간으로 맞춰야 함 - 안 그러면
        // Redis엔 30일 남았는데 토큰 자체는 먼저 만료돼서 refresh가 무의미해짐)
        boolean rememberMe = "true".equals(request.getParameter("rememberMe"));
        int refreshMinutes = rememberMe ? JWTUtil.REFRESH_TOKEN_MINUTES_REMEMBER : JWTUtil.REFRESH_TOKEN_MINUTES_DEFAULT;

        claims.put("rememberMe", rememberMe);

        String accessToken = JWTUtil.generateToken(claims, JWTUtil.ACCESS_TOKEN_MINUTES);
        String refreshToken = JWTUtil.generateToken(claims, refreshMinutes);

        redisTokenService.saveRefreshToken(memberDTO.getEmail(), refreshToken, rememberMe);

        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);

        Gson gson = new Gson();

        String jsonStr = gson.toJson(claims);

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}