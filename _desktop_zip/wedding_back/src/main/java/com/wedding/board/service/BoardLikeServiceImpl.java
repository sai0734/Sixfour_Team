package com.wedding.board.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.board.domain.Board;
import com.wedding.board.domain.BoardLike;
import com.wedding.board.repository.BoardLikeRepository;
import com.wedding.board.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardLikeServiceImpl implements BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;

    private final BoardRepository boardRepository;

    @Override
    @Transactional
    public void like(Long boardId, String memberEmail) {

        boolean exists = boardLikeRepository.existsByBoardIdAndMemberEmail(boardId, memberEmail);

        if (exists) {
            throw new IllegalStateException("이미 좋아요를 누른 게시글입니다.");
        }

        BoardLike boardLike = BoardLike.builder()
                .boardId(boardId)
                .memberEmail(memberEmail)
                .build();

        boardLikeRepository.save(boardLike);

        Board board = boardRepository.findById(boardId).orElseThrow();
        board.increaseLikeCount();
        boardRepository.save(board);
    }

    @Override
    @Transactional
    public void unlike(Long boardId, String memberEmail) {

        Optional<BoardLike> result =
                boardLikeRepository.findByBoardIdAndMemberEmail(boardId, memberEmail);

        BoardLike boardLike = result.orElseThrow();

        boardLikeRepository.delete(boardLike);

        Board board = boardRepository.findById(boardId).orElseThrow();
        board.decreaseLikeCount();
        boardRepository.save(board);
    }

    @Override
    public boolean isLiked(Long boardId, String memberEmail) {

        return boardLikeRepository.existsByBoardIdAndMemberEmail(boardId, memberEmail);
    }

}
