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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.board.dto.BoardDTO;
import com.wedding.board.service.BoardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService service;

    // 전체 목록, ?type=FREE 로 필터링도 가능
    // 예: GET /api/boards/ 또는 GET /api/boards/?type=REVIEW
    @GetMapping("/")
    public List<BoardDTO> list(@RequestParam(name = "type", required = false) String type) {

        if (type == null || type.isBlank()) {
            return service.listAll();
        }

        return service.listByType(type);
    }

    // 베스트 게시글 (좋아요 상위 3개)
    @GetMapping("/best")
    public List<BoardDTO> listBest() {

        return service.listBest();
    }

    // 마이페이지 "내가 쓴 글" - 로그인 필수 (JWTCheckFilter에서 예외 처리 안 해둠)
    // 예: GET /api/boards/member/test@test.com
    @GetMapping("/member/{memberEmail}")
    public List<BoardDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        return service.listByMember(memberEmail);
    }

    @GetMapping("/{boardId}")
    public BoardDTO get(@PathVariable(name = "boardId") Long boardId) {

        return service.get(boardId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody BoardDTO boardDTO, Principal principal) {

        // 작성자는 요청 바디가 아니라 로그인한 본인으로 강제한다 (남의 이름으로 글 작성 방지)
        boardDTO.setMemberEmail(principal.getName());

        log.info("BoardDTO: " + boardDTO);

        Long boardId = service.register(boardDTO);

        return Map.of("boardId", boardId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{boardId}")
    public Map<String, String> modify(
            @PathVariable(name = "boardId") Long boardId,
            @RequestBody BoardDTO boardDTO,
            Principal principal) {

        boardDTO.setBoardId(boardId);

        log.info("Modify: " + boardDTO);

        service.modify(boardDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    // 삭제는 본인 또는 관리자 (게시판 모더레이션 유지)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{boardId}")
    public Map<String, String> remove(
            @PathVariable(name = "boardId") Long boardId,
            Authentication authentication) {

        log.info("Remove(soft): " + boardId);

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        service.remove(boardId, email, isAdmin);

        return Map.of("RESULT", "SUCCESS");
    }

}
