package com.wedding.member.exception;

import lombok.Getter;

// 카카오 로그인 등 CustomUserDetailsService를 거치지 않는 인증 경로에서
// 정지(BLACKLIST)/휴면(DORMANT) 계정을 막을 때 사용
@Getter
public class MemberBlockedException extends RuntimeException {

    private final String errorCode; // ERROR_ACCOUNT_SUSPENDED / ERROR_ACCOUNT_DORMANT
    private final String reason;    // 정지 사유 (없으면 null)
    private final String until;     // 정지 만료 시각 ISO 문자열 (영구정지/해당없음이면 null)

    public MemberBlockedException(String errorCode, String reason, String until) {
        super(errorCode);
        this.errorCode = errorCode;
        this.reason = reason;
        this.until = until;
    }

}