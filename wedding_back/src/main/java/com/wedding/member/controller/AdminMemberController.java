package com.wedding.member.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.dto.AdminMemberDTO;
import com.wedding.member.dto.AdminMemberSearchDTO;
import com.wedding.member.dto.MemberStatusUpdateDTO;
import com.wedding.member.service.AdminMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

    @PutMapping("/{email}/status")
    public Map<String, String> changeStatus(@PathVariable String email,
                                            @Valid @RequestBody MemberStatusUpdateDTO updateDTO) {
        log.info("AdminMemberController_changeStatus 실행: " + email);
        adminMemberService.changeStatus(email, updateDTO);
        return Map.of("result", "success");
    }
}