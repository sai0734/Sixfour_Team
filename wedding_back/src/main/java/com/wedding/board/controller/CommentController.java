package com.wedding.board.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.board.dto.CommentDTO;
import com.wedding.board.service.CommentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService service;

    // 게시글의 댓글+대댓글 전체 목록
    // 예: GET /api/comments/board/1
    @GetMapping("/board/{boardId}")
    public List<CommentDTO> listByBoard(@PathVariable(name = "boardId") Long boardId) {

        return service.listByBoard(boardId);
    }

    @PostMapping("/")
    public Map<String, Long> register(@RequestBody CommentDTO commentDTO) {

        log.info("CommentDTO: " + commentDTO);

        Long commentId = service.register(commentDTO);

        return Map.of("commentId", commentId);
    }

    @PutMapping("/{commentId}")
    public Map<String, String> modify(
            @PathVariable(name = "commentId") Long commentId,
            @RequestBody CommentDTO commentDTO) {

        commentDTO.setCommentId(commentId);

        log.info("Modify: " + commentDTO);

        service.modify(commentDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{commentId}")
    public Map<String, String> remove(@PathVariable(name = "commentId") Long commentId) {

        log.info("Remove(soft): " + commentId);

        service.remove(commentId);

        return Map.of("RESULT", "SUCCESS");
    }

}
