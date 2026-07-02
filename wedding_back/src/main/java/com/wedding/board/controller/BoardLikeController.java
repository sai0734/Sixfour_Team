package com.wedding.board.controller;

import java.util.Map;

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

    // 좋아요 등록
    // 예: POST /api/boardlikes/board/1/member/test@test.com
    @PostMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, String> like(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        log.info("like: " + boardId + ", " + memberEmail);

        service.like(boardId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    // 좋아요 취소
    @DeleteMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, String> unlike(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        log.info("unlike: " + boardId + ", " + memberEmail);

        service.unlike(boardId, memberEmail);

        return Map.of("RESULT", "SUCCESS");
    }

    // 좋아요 눌렀는지 여부 확인 (하트 채우기용)
    @GetMapping("/board/{boardId}/member/{memberEmail}")
    public Map<String, Boolean> isLiked(
            @PathVariable(name = "boardId") Long boardId,
            @PathVariable(name = "memberEmail") String memberEmail) {

        boolean liked = service.isLiked(boardId, memberEmail);

        return Map.of("liked", liked);
    }

}
