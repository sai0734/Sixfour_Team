package com.wedding.product.controller;

import com.wedding.product.dto.QnaDTO;
import com.wedding.product.service.QnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class QnaController {

    private final QnaService qnaService;

    // 상품 질문 목록 조회 (누구나 가능)
    @GetMapping("/{pno}/qna")
    public List<QnaDTO> listByProduct(@PathVariable Long pno) {

        log.info("QnaController_listByProduct 실행~~~~~~~~");

        return qnaService.listByProduct(pno, null);
    }

    // 질문 등록 (로그인 회원 누구나 가능)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/{pno}/qna")
    public Map<String, Long> register(Principal principal, @PathVariable Long pno,
                                      @RequestParam String content) {

        log.info("QnaController_register 실행~~~~~~~~");

        Long qno = qnaService.register(principal.getName(), pno, content);

        return Map.of("qno", qno);
    }

    // 질문 수정 (본인만 가능)
    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{pno}/qna/{qno}")
    public Map<String, String> modify(Principal principal, @PathVariable Long pno, @PathVariable Long qno,
                                      @RequestParam String content) {

        log.info("QnaController_modify 실행~~~~~~~~");

        qnaService.modify(principal.getName(), pno, qno, content);

        return Map.of("RESULT", "SUCCESS");
    }

    // 관리자 답변 등록
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{pno}/qna/{qno}/reply")
    public Map<String, Long> reply(Principal principal, @PathVariable Long pno, @PathVariable Long qno,
                                   @RequestParam String content) {

        log.info("QnaController_reply 실행~~~~~~~~");

        Long replyQno = qnaService.reply(principal.getName(), pno, qno, content);

        return Map.of("qno", replyQno);
    }

    // 관리자 답변 수정
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{pno}/qna/{qno}/reply")
    public Map<String, String> modifyReply(@PathVariable Long pno, @PathVariable Long qno,
                                           @RequestParam String content) {

        log.info("QnaController_modifyReply 실행~~~~~~~~");

        qnaService.modifyReply(pno, qno, content);

        return Map.of("RESULT", "SUCCESS");
    }

    // 질문/답변 삭제 (본인 또는 관리자)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{pno}/qna/{qno}")
    public Map<String, String> remove(Authentication authentication, @PathVariable Long pno, @PathVariable Long qno) {

        log.info("QnaController_remove 실행~~~~~~~~");

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        qnaService.remove(email, isAdmin, pno, qno);

        return Map.of("RESULT", "SUCCESS");
    }

}