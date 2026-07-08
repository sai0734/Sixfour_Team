package com.wedding.board.service;

import java.util.List;

import com.wedding.board.dto.BoardDTO;

public interface BoardService {

    Long register(BoardDTO boardDTO);

    // 상세 조회 - 호출할 때마다 조회수 +1
    BoardDTO get(Long boardId);

    // requesterEmail: 소유자 또는 관리자만 수정/삭제 가능 (isAdmin이 true면 소유자 아니어도 허용)
    void modify(BoardDTO boardDTO, String requesterEmail);

    // 실제 삭제가 아니라 소프트 삭제 (deleted = true)
    void remove(Long boardId, String requesterEmail, boolean isAdmin);

    List<BoardDTO> listAll();

    List<BoardDTO> listByType(String boardType);

    // 마이페이지 "내가 쓴 글" 목록
    List<BoardDTO> listByMember(String memberEmail);

    List<BoardDTO> listBest();

}
