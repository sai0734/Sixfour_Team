package com.wedding.member.service;

import com.wedding.member.domain.Member;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.wedding.member.dto.JoinDTO;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.dto.PasswordResetConfirmDTO;
import com.wedding.member.dto.PasswordResetRequestDTO;
import com.wedding.member.dto.SocialCompleteDTO;

@Transactional
public interface MemberService {

    MemberDTO getKakaoMember(String accessToken);
    void modifyMember(MemberModifyDTO memberModifyDTO);
    void join(JoinDTO joinDTO);
    boolean checkEmailAvailable(String email);
    boolean checkNicknameAvailable(String nickname);

    // 전화번호 상태: AVAILABLE(사용가능) / UNAVAILABLE(단순 중복) / BLOCKED(정지·휴면 회원 번호)
    String getPhoneCheckStatus(String phone);
    void completeSocialProfile(SocialCompleteDTO socialCompleteDTO);
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