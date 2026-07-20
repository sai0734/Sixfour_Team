package com.wedding.member.service;

import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.dto.AdminMemberSearchDTO;
import com.wedding.member.dto.AdminMemberDTO;
import com.wedding.member.dto.MemberStatusUpdateDTO;
import java.util.List;

public interface AdminMemberService {

    PageResponseDTO<AdminMemberDTO> getMemberList(AdminMemberSearchDTO searchDTO);

    void changeStatus(String email, MemberStatusUpdateDTO updateDTO);

    // 일반 사용자 <-> 관리자 권한 변경. requesterEmail은 본인 권한 변경(자기 잠금) 방지용
    void changeRole(String email, String role, String requesterEmail);

    // 관리자 권한을 가진 회원 전체 (회원 관리 "관리자 목록" 탭)
    List<AdminMemberDTO> getAdminList();
}