package com.wedding.member.service;

import com.wedding.member.domain.Member;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;

@Transactional
public interface MemberService {
    
    MemberDTO getKakaoMember(String accessToken);
void modifyMember(MemberModifyDTO memberModifyDTO);

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