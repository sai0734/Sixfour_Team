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

        Map<String, Object> claims  = memberDTO.getClaims();


        String accessToken = JWTUtil.generateToken(claims, 10);
        String refreshToken = JWTUtil.generateToken(claims,60*24);

        // 로그인 유지("rememberMe") 체크 여부 - 프론트에서 넘겨주면 30일, 아니면 7일 TTL
        boolean rememberMe = "true".equals(request.getParameter("rememberMe"));
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