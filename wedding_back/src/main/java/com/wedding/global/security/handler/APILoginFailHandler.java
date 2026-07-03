package com.wedding.global.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.google.gson.Gson;
import com.wedding.member.domain.LoginFail;
import com.wedding.member.domain.Member;
import com.wedding.member.repository.EmailVerifyRepository;
import com.wedding.member.repository.LoginFailRepository;
import com.wedding.member.repository.MemberRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class APILoginFailHandler implements AuthenticationFailureHandler{

    private static final int MAX_FAIL_COUNT = 5;

    private final MemberRepository memberRepository;
    private final LoginFailRepository loginFailRepository;
    private final EmailVerifyRepository emailVerifyRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) 
    throws IOException, ServletException{
        log.info("Login fail...."+ exception);

        String email = request.getParameter("username");
        boolean locked = false;
        boolean pendingVerification = false;
        int failCount = 0;

        if (email != null) {
            Optional<Member> memberOpt = memberRepository.findById(email);

            if (memberOpt.isPresent()) {
                LoginFail loginFail = loginFailRepository.getByMemberEmail(email)
                        .orElseGet(() -> LoginFail.builder().member(memberOpt.get()).build());

                if (!loginFail.isLocked()) {
                    loginFail.increaseFail();

                    if (loginFail.getFailCount() >= MAX_FAIL_COUNT) {
                        loginFail.lock();
                    }

                    loginFailRepository.save(loginFail);
                }

                locked = loginFail.isLocked();
                failCount = loginFail.getFailCount();

            } else {
                // 아직 Member가 없는 경우: 이메일 인증 대기중인 가입 신청이 있는지 확인
                pendingVerification = emailVerifyRepository.findTopByEmailOrderByIdDesc(email)
                        .map(ev -> !ev.isVerified())
                        .orElse(false);
            }
        }

        Gson gson = new Gson();

        String errorCode;
        if (locked) {
            errorCode = "ERROR_ACCOUNT_LOCKED";
        } else if (pendingVerification) {
            errorCode = "ERROR_EMAIL_NOT_VERIFIED";
        } else {
            errorCode = "ERROR_LOGIN";
        }

        String jsonStr = gson.toJson(Map.of(
                "error", errorCode,
                "failCount", failCount,
                "maxFailCount", MAX_FAIL_COUNT
        ));

        response.setContentType("application/json");

        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}