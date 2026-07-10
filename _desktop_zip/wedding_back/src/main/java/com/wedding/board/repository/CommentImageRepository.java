package com.wedding.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.board.domain.CommentImage;

public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {

    List<CommentImage> findByCommentIdOrderBySortOrderAsc(Long commentId);

}
