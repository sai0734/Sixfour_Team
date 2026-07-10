package com.wedding.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 전체 게시글 최신순 (삭제된 글 제외)
    List<Board> findByDeletedFalseOrderByRegDateDesc();

    // 타입별(자유/후기/Q&A) 최신순
    List<Board> findByBoardTypeAndDeletedFalseOrderByRegDateDesc(String boardType);

    // 마이페이지 "내가 쓴 글" - 회원이 작성한 글 최신순 (삭제된 글 제외)
    List<Board> findByMemberEmailAndDeletedFalseOrderByRegDateDesc(String memberEmail);

    // 베스트 게시글 (좋아요 많은 순 상위 3개) - 커뮤허브 "베스트 게시글" 섹션용
    List<Board> findTop3ByDeletedFalseOrderByLikeCountDesc();

    // 더미데이터 중복 삽입 방지용 - 실제 회원 글과 안 섞이도록 전용 memberEmail로 카운트
    long countByMemberEmail(String memberEmail);

    void deleteByMemberEmail(String memberEmail);

    // 댓글 더미데이터 로더가 특정 게시글을 제목으로 찾아 boardId를 얻기 위함
    java.util.Optional<Board> findFirstByTitle(String title);

}
