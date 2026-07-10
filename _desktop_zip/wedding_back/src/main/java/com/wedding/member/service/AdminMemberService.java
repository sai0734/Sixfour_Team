package com.wedding.member.service;

import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.dto.AdminMemberSearchDTO;
import com.wedding.member.dto.AdminMemberDTO;
import com.wedding.member.dto.MemberStatusUpdateDTO;

public interface AdminMemberService {

    PageResponseDTO<AdminMemberDTO> getMemberList(AdminMemberSearchDTO searchDTO);

    void changeStatus(String email, MemberStatusUpdateDTO updateDTO);

}