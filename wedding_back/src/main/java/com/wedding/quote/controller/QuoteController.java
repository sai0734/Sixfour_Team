package com.wedding.quote.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.global.util.CustomFileUtil;
import com.wedding.quote.dto.QuoteCompareResultDTO;
import com.wedding.quote.dto.QuoteDTO;
import com.wedding.quote.service.QuoteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AI 견적서 - 업체 견적서 사진을 업로드하면 AI가 고정 스키마로 정보를 추출하고, 같은 카테고리
// 견적서끼리는 가격/항목/조건 차이점을 뽑아 비교해준다("어느 쪽이 더 좋다"는 판단하지 않음).
// 회원 개인 자료(가격 정보)라 전부 로그인 필수, 이미지 조회도 소유권 확인 후에만 서빙한다.
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final CustomFileUtil fileUtil;

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuoteDTO upload(Principal principal, @RequestPart("file") MultipartFile file) {

        log.info("Quote upload: memberEmail=" + principal.getName());

        return quoteService.uploadAndExtract(principal.getName(), file);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping
    public List<QuoteDTO> listMine(Principal principal) {

        return quoteService.listByMember(principal.getName());
    }

    // 예: GET /api/quotes/compare?ids=1&ids=2 - 정확히 2개, 같은 카테고리여야 함(아니면 400)
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/compare")
    public QuoteCompareResultDTO compare(Principal principal, @RequestParam("ids") List<Long> ids) {

        log.info("Quote compare: memberEmail=" + principal.getName() + ", ids=" + ids);

        return quoteService.compare(principal.getName(), ids);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{quoteId}")
    public Map<String, String> remove(Principal principal, @PathVariable Long quoteId) {

        quoteService.remove(principal.getName(), quoteId);

        return Map.of("RESULT", "SUCCESS");
    }

    // 파일명을 URL에 직접 안 드러내고 quoteId로 받아서, 서비스가 소유권 확인 후에만 실제
    // 파일명을 알아내 서빙한다(board/company 이미지처럼 파일명만 알면 누구나 보는 방식과 다름 -
    // 견적서엔 가격 등 개인 정보가 있어서 본인 확인이 꼭 필요하다는 제품 결정).
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/{quoteId}/image")
    public ResponseEntity<Resource> viewImage(Principal principal, @PathVariable Long quoteId) {

        String fileName = quoteService.getOwnedImageFileName(principal.getName(), quoteId);

        return fileUtil.getFile(fileName);
    }

}
