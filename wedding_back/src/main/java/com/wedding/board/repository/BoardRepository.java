package com.wedding.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 전체 게시글 최신순 (삭제된 글 제외)
    List<Board> findByDeletedFalseOrderByRegDateDesc();

    // 타입별(자유/후기/Q&A) 최신순
    List<Board> findByBoardTypeAndDeletedFalseOrderByRegDateDesc(String boardType);

    // 베스트 게시글 (좋아요 많은 순 상위 3개) - 커뮤허브 "베스트 게시글" 섹션용
    List<Board> findTop3ByDeletedFalseOrderByLikeCountDesc();

}
