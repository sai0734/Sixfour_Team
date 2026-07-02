package com.wedding.companywish.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.companywish.dto.CompanyWishDTO;
import com.wedding.companywish.service.CompanyWishService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/companywishes")
public class CompanyWishController {

    private final CompanyWishService service;

    // 회원의 찜 목록 전체 조회
    // 예: GET /api/companywishes/member/test@test.com
    @GetMapping("/member/{memberEmail}")
    public List<CompanyWishDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        log.info("companyWish list by member: " + memberEmail);

        return service.listByMember(memberEmail);
    }

    @PostMapping("/")
    public Map<String, Long> register(@RequestBody CompanyWishDTO companyWishDTO) {

        log.info("CompanyWishDTO: " + companyWishDTO);

        Long wishId = service.register(companyWishDTO);

        return Map.of("wishId", wishId);
    }

    @DeleteMapping("/{wishId}")
    public Map<String, String> remove(@PathVariable(name = "wishId") Long wishId) {

        log.info("Remove: " + wishId);

        service.remove(wishId);

        return Map.of("RESULT", "SUCCESS");
    }

    // wishId를 모를 때 memberEmail + cmno로 찜 취소 (하트 토글용)
    // 예: DELETE /api/companywishes/member/test@test.com/company/5
    @DeleteMapping("/member/{memberEmail}/company/{cmno}")
    public Map<String, String> removeByMemberAndCompany(
            @PathVariable(name = "memberEmail") String memberEmail,
            @PathVariable(name = "cmno") Long cmno) {

        log.info("Remove by member+company: " + memberEmail + ", " + cmno);

        service.removeByMemberAndCompany(memberEmail, cmno);

        return Map.of("RESULT", "SUCCESS");
    }

}
