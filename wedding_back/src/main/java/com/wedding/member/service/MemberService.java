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