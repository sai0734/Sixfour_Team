package com.wedding.contract.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.contract.dto.ContractReviewResultDTO;
import com.wedding.contract.service.ContractReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/contract")
public class ContractReviewController {

    private final ContractReviewService contractReviewService;

    // 로그인한 회원만 이용 가능 (JWTCheckFilter 기본 인증 적용 - 화이트리스트에 안 넣었음)
    @PostMapping("/review")
    public ContractReviewResultDTO review(@RequestParam("file") MultipartFile file) {

        log.info("contract review request: " + file.getOriginalFilename());

        return contractReviewService.review(file);
    }

}