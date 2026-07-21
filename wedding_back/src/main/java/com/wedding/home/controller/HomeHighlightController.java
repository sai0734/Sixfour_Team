package com.wedding.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.home.dto.MainHighlightsDTO;
import com.wedding.home.service.HomeHighlightService;

import lombok.RequiredArgsConstructor;

// 메인 화면 비로그인 폴라로이드용 - 로그인 여부와 무관하게 항상 공개 조회 가능 (JWTCheckFilter 예외 등록됨)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeHighlightController {

    private final HomeHighlightService homeHighlightService;

    @GetMapping("/highlights")
    public MainHighlightsDTO highlights() {
        return homeHighlightService.getMainHighlights();
    }
}
