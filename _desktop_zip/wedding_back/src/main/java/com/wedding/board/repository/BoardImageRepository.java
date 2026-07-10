package com.wedding.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.BoardImage;

public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    List<BoardImage> findByBoardIdOrderBySortOrderAsc(Long boardId);

}
