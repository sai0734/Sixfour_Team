package com.wedding.product.controller;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.dto.AdminQnaListDTO;
import com.wedding.product.service.QnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/qna")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminQnaController {

    private final QnaService qnaService;

    // 답변 없는 상품 Q&A 목록 (전체 상품 기준)
    @GetMapping("/unanswered")
    public PageResponseDTO<AdminQnaListDTO> listUnanswered(PageRequestDTO requestDTO) {

        log.info("AdminQnaController_listUnanswered 실행~~~~~~~~");

        return qnaService.listUnanswered(requestDTO);
    }

}
