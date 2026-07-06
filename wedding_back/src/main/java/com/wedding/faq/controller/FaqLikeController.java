package com.wedding.faq.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.faq.service.FaqLikeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/faqlikes")
public class FaqLikeController {

    private final FaqLikeService service;

    @PostMapping("/faq/{faqId}/member/{memberEmail}")
    public Map<String, String> like(
            @PathVariable(name = "faqId") Long faqId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        service.like(faqId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/faq/{faqId}/member/{memberEmail}")
    public Map<String, String> unlike(
            @PathVariable(name = "faqId") Long faqId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        service.unlike(faqId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    @GetMapping("/faq/{faqId}/member/{memberEmail}")
    public Map<String, Boolean> isLiked(
            @PathVariable(name = "faqId") Long faqId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        boolean liked = service.isLiked(faqId, memberEmail);

        return Map.of("liked", liked);
    }

}
