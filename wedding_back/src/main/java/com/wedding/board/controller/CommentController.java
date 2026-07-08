package com.wedding.board.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody CommentDTO commentDTO, Principal principal) {

        commentDTO.setMemberEmail(principal.getName());

        log.info("CommentDTO: " + commentDTO);

        Long commentId = service.register(commentDTO);

        return Map.of("commentId", commentId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{commentId}")
    public Map<String, String> modify(
            @PathVariable(name = "commentId") Long commentId,
            @RequestBody CommentDTO commentDTO,
            Principal principal) {

        commentDTO.setCommentId(commentId);

        log.info("Modify: " + commentDTO);

        service.modify(commentDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{commentId}")
    public Map<String, String> remove(
            @PathVariable(name = "commentId") Long commentId,
            Authentication authentication) {

        log.info("Remove(soft): " + commentId);

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        service.remove(commentId, email, isAdmin);

        return Map.of("RESULT", "SUCCESS");
    }

}
