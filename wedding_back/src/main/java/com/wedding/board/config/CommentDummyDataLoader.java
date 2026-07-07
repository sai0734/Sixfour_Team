package com.wedding.board.config;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.wedding.board.domain.Board;
import com.wedding.board.domain.Comment;
import com.wedding.board.repository.BoardRepository;
import com.wedding.board.repository.CommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 게시글 더미데이터(BoardDummyDataLoader) 이후에 실행되어야 하므로 @Order로 순서 보장
@Component
@Order(2)
@RequiredArgsConstructor
@Log4j2
public class CommentDummyDataLoader implements CommandLineRunner {

    private final CommentRepository commentRepository;

    private final BoardRepository boardRepository;

    private static final String DUMMY_EMAIL = "dummy-comment@wedding.local";

    private static final String[] NICKNAMES = {
            "예비신부K", "결혼앞둔직장인2", "곧결혼예정", "신혼준비완료", "예비신랑C",
            "웨딩맘", "6개월전신부", "준비중예비부부", "먼저겪어본선배", "웨딩고민중",
    };

    private int idx = 0;

    @Override
    public void run(String... args) {

        if (commentRepository.countByMemberEmail(DUMMY_EMAIL) > 0) {
            log.info("댓글 더미데이터 이미 존재, 로딩 생략");
            return;
        }

        log.info("댓글 더미데이터 로딩 시작.........");

        comment("청첩장 문구 고민이에요",
                "저는 정형화된 문구보다 저희만의 이야기 담긴 문구로 했어요, 반응이 좋았어요!",
                "오 좋은 팁이네요, 참고할게요!");

        comment("혼주 상견례 언제쯤 하는 게 좋을까요",
                "저희는 예식 8개월 전에 했어요, 너무 이르지 않았나 싶기도 하고 그랬어요.");

        comment("웨딩 준비 순서 정리해봤어요",
                "저도 비슷한 순서로 했는데 청첩장을 좀 더 일찍 준비했어야 했어요.");

        comment("강남권 웨딩홀 후기 궁금해요",
                "저는 근처 홀 다녀왔는데 스태프분들이 친절하셨어요.");

        comment("본식 헤어메이크업 잘하는 곳 추천 부탁드려요",
                "저도 자연스러운 스타일 원해서 알아봤는데 만족스러웠어요.",
                "오 저도 그런 곳 찾고 있었는데 감사해요!");

        comment("결혼 앞두니 잠이 안 와요",
                "저도 그랬어요, 근데 지나고 보니 다 추억이더라고요 ㅎㅎ");

        comment("웨딩드레스 입어보고 눈물 났어요",
                "완전 공감돼요, 저도 그 순간 잊혀지지 않아요.");

        comment("웨딩홀 계약 전 꼭 확인해야 할 것들",
                "취소 위약금 조항도 꼭 확인하세요! 저는 그거 놓쳐서 조금 고생했어요.",
                "오 정보 감사합니다, 저도 다시 확인해볼게요.");

        comment("스드메 계약 시 체크리스트",
                "원본 파일 제공 여부 진짜 중요해요, 저는 그거 협의 안 해서 후회했어요.");

        comment("결혼식 축가 순서 고민 중",
                "저희는 축가 없이 진행했는데 그것도 깔끔하고 좋았어요.");

        comment("웨딩 플래너 이용 후기",
                "저도 플래너 이용 고민 중인데 후기 감사해요.");

        comment("결혼식 D-100 기념 소소한 자랑",
                "축하드려요! 저도 곧 100일이에요 ㅎㅎ",
                "우와 같이 화이팅해요!");

        comment("예산 관리 앱으로 지출 정리하는 팁",
                "앱 이름도 공유해주실 수 있나요?",
                "저는 그냥 이 서비스 예산관리 기능으로 하고 있어요, 편하더라고요.");

        comment("강남 그레이스 웨딩홀 후기 - 가성비 끝판왕",
                "저도 여기 예약했어요! 후기 보니 더 기대되네요.",
                "축하드려요, 분명 만족하실 거예요!");

        comment("특약 조항 꼼꼼히 확인 안 해서 후회한 후기",
                "헐 저도 계약 앞두고 있는데 조심해야겠네요, 알려주셔서 감사합니다.");

        comment("야외 정원 웨딩홀 후기",
                "사진 정말 예쁘겠네요! 저도 야외 웨딩 고민 중이에요.");

        comment("스드메 패키지 가격 공개합니다 - 총 280만원",
                "가격 공유 감사해요! 저희 지역도 비슷한 것 같아요.",
                "지역마다 조금씩 차이는 있더라고요.");

        comment("스드메 업체 응대 아쉬웠던 후기",
                "저도 비슷한 경험 있어요, 계약 전에 후기 꼭 찾아봐야겠어요.");

        comment("본식 스냅 작가님 실력 후기",
                "작가님 성함 여쭤봐도 될까요? 저도 알아보고 싶어요.");

        comment("예복 맞춤 vs 기성복 가격 차이 후기",
                "정보 감사해요! 저는 일단 기성복으로 가려구요.");

        comment("예물 반지 브랜드별 가격 비교해봤어요",
                "저도 비교하고 있었는데 도움 많이 됐어요, 감사합니다.",
                "도움 되셨다니 다행이에요!");

        comment("다이아 등급 공부하고 고른 후기",
                "이런 정보 진짜 필요했어요! 감사합니다.");

        comment("심플한 예물로 만족한 후기",
                "저도 심플하게 하고 싶었는데 후기 보니 더 마음이 굳혀지네요.");

        comment("답례품 뭐가 좋을까요 - 돌잔치랑 다른 점",
                "저도 고민 중인데 실용적인 걸로 가야겠네요.");

        comment("혼인신고 절차 후기",
                "서류 미리 준비해야겠어요, 정보 감사합니다.");

        // ===== 베스트 게시글 후보(좋아요 90개 이상) - 댓글 여러 개 채워둠 =====
        comments("답례품 제작 업체 후기",
                "저도 실용적인 게 좋더라고요, 어떤 제품 고르셨는지 궁금해요!",
                "답례품 고민 중이었는데 도움 많이 됐어요, 감사합니다.",
                "가격대는 어느 정도였나요? 여쭤봐도 될까요?",
                "저희도 비슷한 걸로 알아보고 있어요, 업체명 여쭤봐도 될까요?",
                "실용성 위주 선택 완전 공감해요, 하객분들 반응도 좋으셨나요?",
                "정보 감사합니다! 저장해두고 참고할게요.");

        comments("청첩장 모임 준비 꿀팁 공유해요",
                "청첩장 모임 준비 막막했는데 진짜 도움 되네요!",
                "저도 곧 모임 잡아야 하는데 참고할게요, 감사해요.",
                "혹시 준비물 리스트도 공유해주실 수 있나요?",
                "이런 정보 나눠주셔서 감사해요, 저장했습니다.",
                "저희도 비슷한 시기인데 미리 알아서 다행이에요.",
                "센스 있으시네요! 저도 이렇게 준비해봐야겠어요.");

        comments("잠실 롯데호텔 웨딩홀 이용 후기",
                "저도 여기 알아보고 있었는데 후기 감사해요!",
                "가격대가 어느 정도였는지 여쭤봐도 될까요?",
                "시설 좋다는 말 많이 들었는데 역시 그렇군요.",
                "예산 때문에 고민했는데 후기 보니 더 고민되네요 ㅎㅎ",
                "저희 예산으로는 좀 빠듯할 것 같아요, 그래도 알아볼게요.",
                "사진 좀 더 볼 수 있을까요? 넘 궁금해요!");

        comments("드레스 피팅 꿀팁 후기",
                "붓기 관리 어떻게 하셨는지 여쭤봐도 될까요?",
                "저도 곧 피팅 예정인데 꿀팁 감사해요!",
                "이런 실전 팁 진짜 필요했어요, 감사합니다.",
                "며칠 전부터 관리하신 건가요?",
                "저장해두고 피팅 전에 다시 볼게요, 감사해요.",
                "정보 나눠주셔서 감사해요, 도움 많이 됐어요.");

        comments("체형 고려한 예복 선택 후기",
                "저도 체형 고민 많은데 어떤 핏으로 선택하셨나요?",
                "정보 감사해요, 저도 참고해서 골라볼게요.",
                "체형 커버 잘 되는 브랜드 있으면 공유해주실 수 있나요?",
                "이런 실질적인 후기 진짜 도움 돼요, 감사합니다.",
                "저도 비슷한 고민이었는데 반갑네요 ㅎㅎ",
                "저장했어요! 예복 고를 때 참고할게요.");

        log.info("댓글 더미데이터 로딩 완료");
    }

    // 게시글 하나에 댓글 여러 개(전부 최상위 댓글, 대댓글 없음)를 한 번에 추가
    private void comments(String boardTitle, String... contents) {
        for (String content : contents) {
            addTopLevel(boardTitle, content);
        }
    }

    private void comment(String boardTitle, String content) {
        addTopLevel(boardTitle, content);
    }

    private void comment(String boardTitle, String content, String replyContent) {
        Long parentId = addTopLevel(boardTitle, content);
        if (parentId != null) {
            addReply(boardTitle, parentId, replyContent);
        }
    }

    private Long addTopLevel(String boardTitle, String content) {

        Optional<Board> result = boardRepository.findFirstByTitle(boardTitle);

        if (result.isEmpty()) {
            log.warn("댓글 더미 대상 게시글을 못 찾음: " + boardTitle);
            return null;
        }

        Board board = result.get();

        int i = idx++;

        Comment saved = commentRepository.save(Comment.builder()
                .boardId(board.getBoardId())
                .memberEmail(DUMMY_EMAIL)
                .nickname(NICKNAMES[i % NICKNAMES.length])
                .parentId(null)
                .content(content)
                .regDate(LocalDateTime.now().minusDays(i % 20).minusHours((i * 2) % 24))
                .build());

        board.increaseCommentCount();
        boardRepository.save(board);

        return saved.getCommentId();
    }

    private void addReply(String boardTitle, Long parentId, String content) {

        Optional<Board> result = boardRepository.findFirstByTitle(boardTitle);
        if (result.isEmpty()) return;

        Board board = result.get();

        int i = idx++;

        commentRepository.save(Comment.builder()
                .boardId(board.getBoardId())
                .memberEmail(DUMMY_EMAIL)
                .nickname(NICKNAMES[(i + 3) % NICKNAMES.length])
                .parentId(parentId)
                .content(content)
                .regDate(LocalDateTime.now().minusDays(i % 20).minusHours((i * 2) % 24))
                .build());

        board.increaseCommentCount();
        boardRepository.save(board);
    }

}
