package com.wedding.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글 하나의 댓글+대댓글 전체를 등록순으로 조회 (삭제된 것도 포함 - 대댓글이 부모 잃지 않게, 프론트에서 deleted 여부로 "삭제된 댓글입니다" 처리)
    List<Comment> findByBoardIdOrderByRegDateAsc(Long boardId);

}
