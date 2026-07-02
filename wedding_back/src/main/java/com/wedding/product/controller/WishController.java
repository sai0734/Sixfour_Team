package com.wedding.product.controller;

import com.wedding.product.dto.WishDTO;
import com.wedding.product.service.WishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/wishes")
public class WishController {

    private final WishService wishService;

    // 회원 찜 목록 조회
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/")
    public List<WishDTO> listByMember(Principal principal) {

        log.info("WishController_listByMember 실행~~~~~~~~");

        return wishService.listByMember(principal.getName());

    }

    // 찜 등록
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/product/{pno}")
    public Map<String, Long> register(Principal principal, @PathVariable Long pno) {

        log.info("WishController_register 실행~~~~~~~~");

        Long wno = wishService.register(principal.getName(), pno);

        return Map.of("wno", wno);

    }

    // 찜 취소 (하트 토글용)
    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/product/{pno}")
    public Map<String, String> removeByMemberAndProduct(Principal principal, @PathVariable Long pno) {

        log.info("WishController_removeByMemberAndProduct 실행~~~~~~~~");

        wishService.removeByMemberAndProduct(principal.getName(), pno);

        return Map.of("RESULT", "SUCCESS");

    }

    // 찜 여부 확인 (하트 채우기용)
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/product/{pno}")
    public Map<String, Boolean> isWished(Principal principal, @PathVariable Long pno) {

        log.info("WishController_isWished 실행~~~~~~~~");

        return Map.of("wished", wishService.isWished(principal.getName(), pno));

    }




}
