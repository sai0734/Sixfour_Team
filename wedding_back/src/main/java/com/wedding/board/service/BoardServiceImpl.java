package com.wedding.board.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.board.domain.Board;
import com.wedding.board.dto.BoardDTO;
import com.wedding.board.repository.BoardRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;

    private final ModelMapper modelMapper;

    private final OpenAiClient openAiClient;

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
        // 제목/내용이 바뀌면 캐시된 요약은 더 이상 안 맞으므로 비움 - 다음 조회 시 getAiSummary()가 새로 생성
        board.changeAiSummary(null);

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

    // 게시글 AI 한줄요약 - 이미 있으면 그대로 반환(캐시), 없으면 OpenAI 호출해서 생성 후 저장.
    // 매번 새로 부르면 비용/속도 문제가 있어서, 글 하나당 딱 한 번만 생성되도록 board.aiSummary에 캐시함.
    // 프론트에서 목록 조회 시 자동 호출하지 않고, 사용자가 "AI 요약" 버튼을 눌렀을 때만 호출하는 방식 전제.
    private static final String SUMMARY_SYSTEM_PROMPT =
            "당신은 커뮤니티 게시글을 한 문장으로 요약하는 도우미입니다. " +
                    "주어진 게시글 제목과 본문을 읽고, 글쓴이가 무엇을 하려는 글인지(의견을 구함/경험을 공유함/정보를 알림 등) " +
                    "한국어 한 문장(40자 내외)으로, 반드시 '~글입니다.'로 끝나도록 요약하세요.\n\n" +
                    "지켜야 할 규칙:\n" +
                    "1. 본문에 없는 내용은 절대 지어내지 마세요. 특히 본문이 질문이면, 그 답을 아는 것처럼 " +
                    "일반적인 조언이나 정답을 만들어서 쓰지 마세요. (예: 글쓴이가 '6개월 전에 했는데 다들 비슷한가요?' " +
                    "라고 물었다면, 요약은 '자신의 경험을 공유하며 다른 사람들의 시기를 물어보는 글'이어야 하고, " +
                    "'6개월 전이 좋습니다' 같은 지어낸 답을 쓰면 안 됩니다.)\n" +
                    "2. 글에 질문이 있다고 해서 그 질문을 비슷한 말로 반복하지 마세요. " +
                    "'~에 대해 다른 사람들의 의견을 구하는 글', '~한 경험을 공유하는 글'처럼 " +
                    "이 글의 목적과 핵심 내용을 설명하세요.\n" +
                    "3. 인사말, 따옴표, 물음표 없이 '~글입니다.'로 끝나는 요약 문장 하나만 출력하세요.";

    @Override
    public String getAiSummary(Long boardId) {

        Board board = boardRepository.findById(boardId).orElseThrow();

        if (board.getAiSummary() != null && !board.getAiSummary().isBlank()) {
            return board.getAiSummary();
        }

        List<OpenAiMessageDTO> messages = List.of(
                OpenAiMessageDTO.of("system", SUMMARY_SYSTEM_PROMPT),
                OpenAiMessageDTO.of("user", "제목: " + board.getTitle() + "\n내용: " + board.getContent())
        );

        OpenAiResponseDTO response = openAiClient.getChatCompletions(messages, null);
        String summary = response.getChoices().getFirst().getMessage().getContent().trim();

        board.changeAiSummary(summary);
        boardRepository.save(board);

        return summary;
    }

}
