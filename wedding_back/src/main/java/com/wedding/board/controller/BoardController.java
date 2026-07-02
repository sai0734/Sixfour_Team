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
    // 예: GET /api/boards/ 또는 GET /api/boards/?type=QNA
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

    @GetMapping("/{boardId}")
    public BoardDTO get(@PathVariable(name = "boardId") Long boardId) {

        return service.get(boardId);
    }

    @PostMapping("/")
    public Map<String, Long> register(@RequestBody BoardDTO boardDTO) {

        log.info("BoardDTO: " + boardDTO);

        Long boardId = service.register(boardDTO);

        return Map.of("boardId", boardId);
    }

    @PutMapping("/{boardId}")
    public Map<String, String> modify(
            @PathVariable(name = "boardId") Long boardId,
            @RequestBody BoardDTO boardDTO) {

        boardDTO.setBoardId(boardId);

        log.info("Modify: " + boardDTO);

        service.modify(boardDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{boardId}")
    public Map<String, String> remove(@PathVariable(name = "boardId") Long boardId) {

        log.info("Remove(soft): " + boardId);

        service.remove(boardId);

        return Map.of("RESULT", "SUCCESS");
    }

}
