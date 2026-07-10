package com.wedding.faq.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.faq.dto.FaqDTO;
import com.wedding.faq.service.FaqService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService service;

    // 전체 목록, ?category=예산 으로 필터링도 가능
    @GetMapping("/")
    public List<FaqDTO> list(@RequestParam(name = "category", required = false) String category) {

        if (category == null || category.isBlank()) {
            return service.listAll();
        }

        return service.listByCategory(category);
    }

    @GetMapping("/{faqId}")
    public FaqDTO get(@PathVariable(name = "faqId") Long faqId) {

        return service.get(faqId);
    }

    // 등록/수정/삭제는 관리자 페이지에서만 - 팀 큐레이션(또는 나중에 AI 생성)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody FaqDTO faqDTO) {

        log.info("FaqDTO: " + faqDTO);

        Long faqId = service.register(faqDTO);

        return Map.of("faqId", faqId);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{faqId}")
    public Map<String, String> modify(
            @PathVariable(name = "faqId") Long faqId,
            @RequestBody FaqDTO faqDTO) {

        faqDTO.setFaqId(faqId);

        log.info("Modify: " + faqDTO);

        service.modify(faqDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{faqId}")
    public Map<String, String> remove(@PathVariable(name = "faqId") Long faqId) {

        log.info("Remove: " + faqId);

        service.remove(faqId);

        return Map.of("RESULT", "SUCCESS");
    }

}
