package com.wedding.member.dto;

// 카카오 인증(getKakaoUserInfo) 이후 처리 결과. status는 셋 중 하나.
//
// READY          -> 이미 연동(SocialAccount)까지 되어있는 완료 회원. memberDTO로 바로 JWT 발급(로그인)
// CONFIRM_LINK   -> 연동 기록은 없지만, 카카오 계정 이메일과 똑같은 이메일의 기존(가입완료) 회원이 있음.
//                   조용히 자동 연동/로그인시키지 않고, "이 계정과 연동할까요?" 확인을 먼저 받아야 함.
//                   token은 그 확인(confirm) 처리에 쓰는 짧은 토큰.
// PENDING_SIGNUP -> 신규 또는 예전에 시작만 하고 안 끝낸 가입. token은 가입 마무리(pendingToken)에 쓰는 토큰.
public record KakaoAuthResultDTO(
        String status,
        MemberDTO memberDTO,
        String token,
        String kakaoEmail
) {
}