package com.wedding.board.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.board.domain.Board;
import com.wedding.board.domain.Comment;
import com.wedding.board.dto.CommentDTO;
import com.wedding.board.repository.BoardRepository;
import com.wedding.board.repository.CommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    private final BoardRepository boardRepository;

    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Long register(CommentDTO commentDTO) {

        log.info("comment register.........");

        Comment comment = modelMapper.map(commentDTO, Comment.class);

        Comment savedComment = commentRepository.save(comment);

        Board board = boardRepository.findById(commentDTO.getBoardId()).orElseThrow();
        board.increaseCommentCount();
        boardRepository.save(board);

        return savedComment.getCommentId();
    }

    @Override
    public void modify(CommentDTO commentDTO) {

        Optional<Comment> result = commentRepository.findById(commentDTO.getCommentId());

        Comment comment = result.orElseThrow();

        comment.changeContent(commentDTO.getContent());

        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void remove(Long commentId) {

        Optional<Comment> result = commentRepository.findById(commentId);

        Comment comment = result.orElseThrow();

        if (!comment.isDeleted()) {
            comment.changeDeleted(true);
            commentRepository.save(comment);

            Board board = boardRepository.findById(comment.getBoardId()).orElseThrow();
            board.decreaseCommentCount();
            boardRepository.save(board);
        }
    }

    @Override
    public List<CommentDTO> listByBoard(Long boardId) {

        List<Comment> result =
                commentRepository.findByBoardIdOrderByRegDateAsc(boardId);

        return result.stream()
                .map(comment -> modelMapper.map(comment, CommentDTO.class))
                .collect(Collectors.toList());
    }

}
