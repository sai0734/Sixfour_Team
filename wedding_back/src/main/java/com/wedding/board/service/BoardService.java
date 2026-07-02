package com.wedding.board.service;

import java.util.List;

import com.wedding.board.dto.BoardDTO;

public interface BoardService {

    Long register(BoardDTO boardDTO);

    // 상세 조회 - 호출할 때마다 조회수 +1
    BoardDTO get(Long boardId);

    void modify(BoardDTO boardDTO);

    // 실제 삭제가 아니라 소프트 삭제 (deleted = true)
    void remove(Long boardId);

    List<BoardDTO> listAll();

    List<BoardDTO> listByType(String boardType);

    List<BoardDTO> listBest();

}
