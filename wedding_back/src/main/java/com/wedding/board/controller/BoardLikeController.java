package com.wedding.board.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.board.service.BoardLikeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/boardlikes")
public class BoardLikeController {

    private final BoardLikeService service;

    // 좋아요 등록 - 경로의 memberEmail이 로그인한 본인과 같은지 확인
    // (URL 모양은 프론트 호환을 위해 그대로 두고, 서버에서 위조 여부만 검증)
    // 예: POST /api/boardlikes/board/1/member/test@test.com
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, String> like(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail,
            Principal principal) {

        if (!principal.getName().equals(memberEmail)) {
            throw new IllegalStateException("본인 계정으로만 좋아요를 누를 수 있습니다.");
        }

        log.info("like: " + boardId + ", " + memberEmail);

        service.like(boardId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    // 좋아요 취소 - 마찬가지로 본인 계정인지 확인
    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, String> unlike(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail,
            Principal principal) {

        if (!principal.getName().equals(memberEmail)) {
            throw new IllegalStateException("본인 계정으로만 좋아요를 취소할 수 있습니다.");
        }

        log.info("unlike: " + boardId + ", " + memberEmail);

        service.unlike(boardId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    // 좋아요 눌렀는지 여부 확인 (하트 채우기용) - 단순 조회라 그대로 둠
    @GetMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, Boolean> isLiked(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        boolean liked = service.isLiked(boardId, memberEmail);

        return Map.of("liked", liked);
    }

}
