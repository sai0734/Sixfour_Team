package com.wedding.faq.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/faq/{faqId}/member/{memberEmail}")
    public Map<String, String> like(
            @PathVariable(name = "faqId") Long faqId,
            @PathVariable(name = "memberEmail") String memberEmail,
            Principal principal) {

        if (!principal.getName().equals(memberEmail)) {
            throw new IllegalStateException("본인 계정으로만 좋아요를 누를 수 있습니다.");
        }

        service.like(faqId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/faq/{faqId}/member/{memberEmail}")
    public Map<String, String> unlike(
            @PathVariable(name = "faqId") Long faqId,
            @PathVariable(name = "memberEmail") String memberEmail,
            Principal principal) {

        if (!principal.getName().equals(memberEmail)) {
            throw new IllegalStateException("본인 계정으로만 좋아요를 취소할 수 있습니다.");
        }

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
