package com.wedding.board.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.board.domain.Board;
import com.wedding.board.dto.BoardDTO;
import com.wedding.board.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(BoardDTO boardDTO) {

        log.info("board register.........");

        Board board = modelMapper.map(boardDTO, Board.class);

        Board savedBoard = boardRepository.save(board);

        return savedBoard.getBoardId();
    }

    @Override
    public BoardDTO get(Long boardId) {

        Optional<Board> result = boardRepository.findById(boardId);

        Board board = result.orElseThrow();

        // 상세 조회할 때마다 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return modelMapper.map(board, BoardDTO.class);
    }

    @Override
    public void modify(BoardDTO boardDTO, String requesterEmail) {

        Optional<Board> result = boardRepository.findById(boardDTO.getBoardId());

        Board board = result.orElseThrow();

        if (!board.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        board.changeTitle(boardDTO.getTitle());
        board.changeContent(boardDTO.getContent());
        board.changeCategory(boardDTO.getCategory());
        board.changeRating(boardDTO.getRating());

        boardRepository.save(board);
    }

    @Override
    public void remove(Long boardId, String requesterEmail, boolean isAdmin) {

        Optional<Board> result = boardRepository.findById(boardId);

        Board board = result.orElseThrow();

        if (!isAdmin && !board.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        board.changeDeleted(true);

        boardRepository.save(board);
    }

    @Override
    public List<BoardDTO> listAll() {

        List<Board> result = boardRepository.findByDeletedFalseOrderByRegDateDesc();

        return result.stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardDTO> listByType(String boardType) {

        List<Board> result =
                boardRepository.findByBoardTypeAndDeletedFalseOrderByRegDateDesc(boardType);

        return result.stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardDTO> listByMember(String memberEmail) {

        List<Board> result =
                boardRepository.findByMemberEmailAndDeletedFalseOrderByRegDateDesc(memberEmail);

        return result.stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardDTO> listBest() {

        List<Board> result = boardRepository.findTop3ByDeletedFalseOrderByLikeCountDesc();

        return result.stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

}
