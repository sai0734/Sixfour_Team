package com.wedding.member.service;

import com.wedding.member.domain.Member;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.wedding.member.dto.JoinDTO;
import com.wedding.member.dto.KakaoAuthResultDTO;
import com.wedding.member.dto.KakaoLinkConfirmDTO;
import com.wedding.member.dto.KakaoSignupCompleteDTO;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.dto.PasswordResetConfirmDTO;
import com.wedding.member.dto.PasswordResetRequestDTO;

@Transactional
public interface MemberService {

    // 카카오 인증 직후 호출 - 가입 완료된 회원이면 바로 로그인 처리 가능한 결과를,
    // 아니면(신규/미완료) 로그인시키지 않고 pendingToken만 담은 결과를 돌려줌
    KakaoAuthResultDTO processKakaoAuth(String accessToken);

    // pendingToken + 추가정보로 카카오 가입을 최종 완료 (이 시점에 비로소 로그인 처리, JWT 발급까지 포함)
    java.util.Map<String, Object> completeKakaoSignup(KakaoSignupCompleteDTO dto);

    // CONFIRM_LINK 상태에서 사용자가 "연동할게요"를 선택했을 때 - 그 자리에서 연동 처리 + 로그인(JWT 발급)까지
    java.util.Map<String, Object> confirmKakaoEmailLink(KakaoLinkConfirmDTO dto);

    void modifyMember(MemberModifyDTO memberModifyDTO);
    void join(JoinDTO joinDTO);
    boolean checkEmailAvailable(String email);
    boolean checkNicknameAvailable(String nickname);

    // 전화번호 상태: AVAILABLE(사용가능) / UNAVAILABLE(단순 중복) / BLOCKED(정지·휴면 회원 번호)
    String getPhoneCheckStatus(String phone);
    boolean hasProfile(String email);
    String verifyEmail(String token);
    String resendVerification(String email);
    void requestPasswordReset(PasswordResetRequestDTO requestDTO);
    String confirmPasswordReset(PasswordResetConfirmDTO confirmDTO);

    // 회원탈퇴
    void withdrawMember(String email);

    // 소셜(카카오) 계정 연동 추가/해제 및 조회
    void linkKakaoAccount(String email, String kakaoAccessToken);
    void unlinkSocialAccount(String email, String provider);
    java.util.List<String> getLinkedProviders(String email);

    default MemberDTO entityToDTO(Member member){

        MemberDTO dto = new MemberDTO(
                member.getEmail(),
                member.getPw(),
                member.getNickname(),
                member.isSocial(),
                member.getMemberRoleList().stream()
                        .map(memberRole -> memberRole.name()).collect(Collectors.toList()));
        return dto;
    }
}