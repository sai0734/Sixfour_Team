package com.wedding.member.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.dto.AdminMemberDTO;
import com.wedding.member.dto.AdminMemberSearchDTO;
import com.wedding.member.dto.MemberRoleUpdateDTO;
import com.wedding.member.dto.MemberStatusUpdateDTO;
import com.wedding.member.service.AdminMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public PageResponseDTO<AdminMemberDTO> list(AdminMemberSearchDTO searchDTO) {
        log.info("AdminMemberController_list 실행: " + searchDTO);
        return adminMemberService.getMemberList(searchDTO);
    }

    @GetMapping("/admins")
    public List<AdminMemberDTO> adminList() {
        log.info("AdminMemberController_adminList 실행");
        return adminMemberService.getAdminList();
    }

    @PutMapping("/{email}/status")
    public Map<String, String> changeStatus(@PathVariable String email,
                                            @Valid @RequestBody MemberStatusUpdateDTO updateDTO) {
        log.info("AdminMemberController_changeStatus 실행: " + email);
        adminMemberService.changeStatus(email, updateDTO);
        return Map.of("result", "success");
    }

    @PutMapping("/{email}/role")
    public Map<String, String> changeRole(@PathVariable String email,
                                          @Valid @RequestBody MemberRoleUpdateDTO updateDTO,
                                          Principal principal) {
        log.info("AdminMemberController_changeRole 실행: " + email);
        adminMemberService.changeRole(email, updateDTO.getRole(), principal.getName());
        return Map.of("result", "success");
    }
}