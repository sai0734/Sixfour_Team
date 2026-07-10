package com.wedding.checklist.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.checklist.dto.ChecklistDTO;
import com.wedding.checklist.service.ChecklistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/checklists")
public class ChecklistController {

    private final ChecklistService service;

    @GetMapping("/{checklistId}")
    public ChecklistDTO get(@PathVariable(name = "checklistId") Long checklistId) {

        return service.get(checklistId);
    }

    // 특정 회원의 체크리스트 전체 조회
    // 예: GET /api/checklists/member/test@test.com
    @GetMapping("/member/{memberEmail}")
    public List<ChecklistDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        log.info("checklist list by member: " + memberEmail);

        return service.listByMember(memberEmail);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody ChecklistDTO checklistDTO, Principal principal) {

        checklistDTO.setMemberEmail(principal.getName());

        log.info("ChecklistDTO: " + checklistDTO);

        Long checklistId = service.register(checklistDTO);

        return Map.of("checklistId", checklistId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{checklistId}")
    public Map<String, String> modify(
            @PathVariable(name = "checklistId") Long checklistId,
            @RequestBody ChecklistDTO checklistDTO,
            Principal principal) {

        checklistDTO.setChecklistId(checklistId);

        log.info("Modify: " + checklistDTO);

        service.modify(checklistDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{checklistId}")
    public Map<String, String> remove(
            @PathVariable(name = "checklistId") Long checklistId,
            Principal principal) {

        log.info("Remove: " + checklistId);

        service.remove(checklistId, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

}
