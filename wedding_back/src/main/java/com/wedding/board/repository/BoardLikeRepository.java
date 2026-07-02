package com.wedding.board.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.BoardLike;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByBoardIdAndMemberEmail(Long boardId, String memberEmail);

    Optional<BoardLike> findByBoardIdAndMemberEmail(Long boardId, String memberEmail);

}
