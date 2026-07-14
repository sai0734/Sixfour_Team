package com.wedding.companywish.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.companywish.service.CompanyWishService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/companywishes")
public class CompanyWishController {

    private final CompanyWishService service;

    /** 현재 로그인 사용자가 해당 업체를 찜했는지 확인 */
    @GetMapping("/{cmno}/check")
    public Map<String, Boolean> check(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        boolean liked = service.check(authentication.getName(), cmno);
        return Map.of("liked", liked);
    }

    /** 업체 찜 등록 */
    @PostMapping("/{cmno}")
    public Map<String, Object> add(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        log.info("company wish add: email={}, cmno={}", authentication.getName(), cmno);
        service.add(authentication.getName(), cmno);
        return Map.of("result", "success", "liked", true);
    }

    /** 업체 찜 해제 */
    @DeleteMapping("/{cmno}")
    public Map<String, Object> remove(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        log.info("company wish remove: email={}, cmno={}", authentication.getName(), cmno);
        service.remove(authentication.getName(), cmno);
        return Map.of("result", "success", "liked", false);
    }

    /** 마이페이지 - 로그인 사용자의 찜 업체 목록 조회 */
    @GetMapping
    public List<CompanyDTO> list(Authentication authentication) {

        log.info("company wish list: email={}", authentication.getName());
        return service.getMyCompanies(authentication.getName());
    }
}
