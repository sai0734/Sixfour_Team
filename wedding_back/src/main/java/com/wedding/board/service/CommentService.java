package com.wedding.board.service;

import java.util.List;

import com.wedding.board.dto.CommentDTO;

public interface CommentService {

    Long register(CommentDTO commentDTO);

    void modify(CommentDTO commentDTO);

    // 소프트 삭제. 대댓글이 달려있어도 그대로 두고(자식은 살아있음), 화면에서 "삭제된 댓글입니다" 처리
    void remove(Long commentId);

    List<CommentDTO> listByBoard(Long boardId);

}
