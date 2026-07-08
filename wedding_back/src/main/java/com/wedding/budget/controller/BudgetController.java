package com.wedding.budget.controller;

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

import com.wedding.budget.dto.BudgetDTO;
import com.wedding.budget.service.BudgetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    @GetMapping("/{budgetId}")
    public BudgetDTO get(@PathVariable(name = "budgetId") Long budgetId) {

        return service.get(budgetId);
    }

    // 특정 회원의 예산 항목 전체 조회
    // 예: GET /api/budgets/member/test@test.com
    @GetMapping("/member/{memberEmail}")
    public List<BudgetDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        log.info("budget list by member: " + memberEmail);

        return service.listByMember(memberEmail);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody BudgetDTO budgetDTO, Principal principal) {

        budgetDTO.setMemberEmail(principal.getName());

        log.info("BudgetDTO: " + budgetDTO);

        Long budgetId = service.register(budgetDTO);

        return Map.of("budgetId", budgetId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{budgetId}")
    public Map<String, String> modify(
            @PathVariable(name = "budgetId") Long budgetId,
            @RequestBody BudgetDTO budgetDTO,
            Principal principal) {

        budgetDTO.setBudgetId(budgetId);

        log.info("Modify: " + budgetDTO);

        service.modify(budgetDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{budgetId}")
    public Map<String, String> remove(
            @PathVariable(name = "budgetId") Long budgetId,
            Principal principal) {

        log.info("Remove: " + budgetId);

        service.remove(budgetId, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

}
