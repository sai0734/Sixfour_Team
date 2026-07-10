package com.wedding.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 카카오 인증 직후 발급된 pendingToken을 들고, 추가정보 입력을 완료할 때 쓰는 요청.
// 이 요청이 성공해야 비로소 진짜 Member(+MemberDetail+TermsAgree)가 만들어지고 JWT가 발급됨.
// 로그인 전 상태에서 호출하는 API라 email 필드가 없고, 그 대신 pendingToken 안에
// 서버가 이미 검증해둔 카카오 이메일이 서명되어 들어있음 (클라이언트가 이메일을 위조할 수 없게)
@Data
public class KakaoSignupCompleteDTO {

    @NotBlank
    private String pendingToken;

    @NotBlank
    private String nickname;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private LocalDate birthDate;

    private String zipCode;

    @NotBlank
    private String address;

    private String addressDetail;

    private boolean termsAgree;

    private boolean privacyAgree;

    private boolean marketing;

}