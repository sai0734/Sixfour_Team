package com.wedding.weddingplan.controller;

import java.security.Principal;
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

import com.wedding.weddingplan.dto.WeddingPlanDTO;
import com.wedding.weddingplan.service.WeddingPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/weddingplans")
public class WeddingPlanController {

    private final WeddingPlanService service;

    // 1:1 관계라 회원 이메일로 단건 조회 (마이페이지 허브 진입 시 사용)
    // 예: GET /api/weddingplans/member/test@test.com
    @GetMapping("/member/{memberEmail}")
    public WeddingPlanDTO getByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        log.info("weddingPlan get by member: " + memberEmail);

        return service.getByMember(memberEmail);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody WeddingPlanDTO weddingPlanDTO, Principal principal) {

        weddingPlanDTO.setMemberEmail(principal.getName());

        log.info("WeddingPlanDTO: " + weddingPlanDTO);

        Long weddingPlanId = service.register(weddingPlanDTO);

        return Map.of("weddingPlanId", weddingPlanId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{weddingPlanId}")
    public Map<String, String> modify(
            @PathVariable(name = "weddingPlanId") Long weddingPlanId,
            @RequestBody WeddingPlanDTO weddingPlanDTO,
            Principal principal) {

        weddingPlanDTO.setWeddingPlanId(weddingPlanId);

        log.info("Modify: " + weddingPlanDTO);

        service.modify(weddingPlanDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{weddingPlanId}")
    public Map<String, String> remove(
            @PathVariable(name = "weddingPlanId") Long weddingPlanId,
            Principal principal) {

        log.info("Remove: " + weddingPlanId);

        service.remove(weddingPlanId, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

}
