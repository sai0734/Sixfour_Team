package com.wedding.companywish.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.companywish.dto.CompanyWishItemDTO;
import com.wedding.companywish.service.CompanyWishService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/companywishes")
public class CompanyWishController {

    private final CompanyWishService service;

    /** 현재 로그인 사용자가 해당 업체를 찜했는지 확인 (옵션 상관없이 하나라도 찜했으면 true) */
    @GetMapping("/{cmno}/check")
    public Map<String, Boolean> check(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        boolean liked = service.check(authentication.getName(), cmno);
        return Map.of("liked", liked);
    }

    /**
     * 업체 찜 등록.
     * 재원 추가 - optionName/optionAmount/optionImage 쿼리파라미터로 홀/드레스/메이크업
     * 옵션(가격/이미지 포함)과 함께 찜할 수 있음. 옵션 없이 호출하면 기존과 동일하게 동작
     * (스튜디오 등 옵션 없는 업체).
     */
    @PostMapping("/{cmno}")
    public Map<String, Object> add(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno,
            @RequestParam(name = "optionName", required = false, defaultValue = "") String optionName,
            @RequestParam(name = "optionAmount", required = false, defaultValue = "0") Integer optionAmount,
            @RequestParam(name = "optionImage", required = false) String optionImage) {

        log.info("company wish add: email={}, cmno={}, optionName={}, optionAmount={}",
                authentication.getName(), cmno, optionName, optionAmount);
        service.addWithOption(authentication.getName(), cmno, optionName, optionAmount, optionImage);
        return Map.of("result", "success", "liked", true);
    }

    /** 업체 찜 해제 (하트 토글 - 옵션 상관없이 해당 업체 찜 전부 삭제) */
    @DeleteMapping("/{cmno}")
    public Map<String, Object> remove(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        log.info("company wish remove: email={}, cmno={}", authentication.getName(), cmno);
        service.remove(authentication.getName(), cmno);
        return Map.of("result", "success", "liked", false);
    }

    // 재원 추가 - 마이페이지 카드/전체선택 삭제는 옵션별로 여러 건일 수 있어서 wishId로 정확히 삭제
    @DeleteMapping("/wish/{wishId}")
    public Map<String, String> removeByWishId(
            Authentication authentication,
            @PathVariable(name = "wishId") Long wishId) {

        log.info("company wish remove by wishId: email={}, wishId={}", authentication.getName(), wishId);
        service.removeByWishId(authentication.getName(), wishId);
        return Map.of("RESULT", "SUCCESS");
    }

    // 재원 추가 - 업체 상세페이지 옵션 하트(찜) 표시용 - 이 업체에서 내가 찜한 옵션명 전체
    @GetMapping("/{cmno}/wished-options")
    public List<String> getWishedOptionNames(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno) {

        return service.getWishedOptionNames(authentication.getName(), cmno);
    }

    // 재원 추가 - 옵션 하트 토글 시 "찜 해제"용 - 같은 업체의 다른 옵션 찜은 그대로 두고 이 옵션만 지움
    @DeleteMapping("/{cmno}/option")
    public Map<String, Object> removeOption(
            Authentication authentication,
            @PathVariable(name = "cmno") Long cmno,
            @RequestParam(name = "optionName") String optionName) {

        log.info("company wish option remove: email={}, cmno={}, optionName={}",
                authentication.getName(), cmno, optionName);
        service.removeOption(authentication.getName(), cmno, optionName);
        return Map.of("result", "success", "liked", false);
    }

    /**
     * 마이페이지 - 로그인 사용자의 찜 업체 목록 조회.
     * 재원 수정 - 옵션별로 여러 건 찜이 가능해지면서 wishId/optionName이 필요해져
     * List&lt;CompanyDTO&gt; 대신 List&lt;CompanyWishItemDTO&gt;로 응답 형태 변경.
     */
    @GetMapping
    public List<CompanyWishItemDTO> list(Authentication authentication) {

        log.info("company wish list: email={}", authentication.getName());
        return service.getMyCompanyWishItems(authentication.getName());
    }
}
