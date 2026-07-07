package com.wedding.board.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.wedding.board.domain.Board;
import com.wedding.board.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 실제 회원 계정과 무관한 표시용 닉네임으로 채워진 더미 게시글.
// 서버 부팅 시 한 번만 실행되고, 이미 있으면 건너뜀(전용 memberEmail로 판별).
// @Order(1): CommentDummyDataLoader(@Order(2))보다 반드시 먼저 실행되어야 함
@Component
@Order(1)
@RequiredArgsConstructor
@Log4j2
public class BoardDummyDataLoader implements CommandLineRunner {

    private final BoardRepository boardRepository;

    private static final String DUMMY_EMAIL = "dummy-board@wedding.local";

    private static final String[] NICKNAMES = {
            "웨딩고수", "예비신부A", "예비신랑B", "준비중신랑", "신부이지영",
            "곧결혼", "D100일전", "신혼준비생", "웨딩플래너지망생", "결혼앞둔직장인",
            "예비부부한쌍", "셀프웨딩러", "알뜰준비중", "웨딩헬퍼", "하객백명맘",
            "스몰웨딩러", "야외웨딩꿈나무", "결혼앞둔막내", "두번째상견례", "신부대기중",
    };

    private int idx = 0;

    @Override
    public void run(String... args) {

        if (boardRepository.countByMemberEmail(DUMMY_EMAIL) > 0) {
            log.info("게시판 더미데이터 이미 존재, 로딩 생략");
            return;
        }

        log.info("게시판 더미데이터 100개 로딩 시작.........");

        List<Board> boards = new ArrayList<>();

        addFree(boards, "웨딩준비", "청첩장 문구 고민이에요",
                "진심 담긴 문구 vs 정형화된 문구 어떤 게 나을지 고민되네요. 다들 어떻게 쓰셨나요?");
        addFree(boards, "웨딩준비", "혼주 상견례 언제쯤 하는 게 좋을까요",
                "예식 6개월 전쯤 했는데 다들 비슷한 시기에 하시나요?");
        addFree(boards, "웨딩준비", "웨딩 준비 순서 정리해봤어요",
                "홀 계약 → 스드메 → 청첩장 → 예물 순으로 진행 중인데 이 순서 맞나요?");
        addFree(boards, "웨딩준비", "예단이랑 예물 순서가 헷갈려요",
                "양가 상견례 후에 진행하면 되는 걸까요? 경험자분들 조언 부탁드려요.");
        addFree(boards, "웨딩준비", "결혼 준비하면서 제일 힘들었던 부분",
                "저는 예산 맞추는 게 제일 어렵더라고요. 다들 어떠셨어요?");
        addFree(boards, "웨딩준비", "신혼여행 시기 언제로 잡으셨나요",
                "예식 직후 vs 한 달 뒤, 장단점이 궁금해요.");
        addFree(boards, "웨딩준비", "웨딩 준비 체크리스트 공유해요",
                "저 스스로 정리한 체크리스트인데 도움 되실까 해서 올려요.");
        addFree(boards, "웨딩준비", "본식 스냅 작가님 언제 정하셨나요",
                "저는 너무 늦게 알아봐서 마음에 드는 곳 예약이 꽉 찼더라고요.");
        addFree(boards, "웨딩준비", "혼수 준비는 언제부터 시작하시나요",
                "신혼집 입주 한 달 전부터 하려는데 너무 늦은 걸까요?");
        addFree(boards, "웨딩준비", "결혼식 사회자 섭외 고민",
                "지인 vs 전문 사회자, 다들 어떻게 결정하셨어요?");
        addFree(boards, "웨딩준비", "웨딩 준비 다이어리 앱 추천해주세요",
                "일정 관리하기 편한 앱 있으면 공유 부탁드려요.");
        addFree(boards, "웨딩준비", "부케 준비물 리스트가 궁금해요",
                "생화 부케 예약은 언제쯤 하는 게 좋을까요?");
        addFree(boards, "웨딩준비", "결혼식 축가 순서 고민 중",
                "지인 축가 넣을지 말지 고민되네요, 다들 어떻게 하셨나요?");

        addFree(boards, "업체정보", "강남권 웨딩홀 후기 궁금해요",
                "견적 받아둔 곳이 몇 군데 있는데 실제 이용 후기 듣고 싶어요.");
        addFree(boards, "업체정보", "스드메 패키지 vs 개별 진행 뭐가 나을까요",
                "가격 차이가 크게 나던데 경험자분들 의견 궁금합니다.");
        addFree(boards, "업체정보", "드레스샵 투어 몇 곳이나 다니셨나요",
                "저는 3곳 정도 생각 중인데 너무 적을까요?");
        addFree(boards, "업체정보", "본식 헤어메이크업 잘하는 곳 추천 부탁드려요",
                "자연스러운 스타일 선호하는데 잘 아시는 분 계실까요?");
        addFree(boards, "업체정보", "웨딩카 대여 업체 후기 나눠요",
                "가격대별로 알아본 거 공유할게요.");
        addFree(boards, "업체정보", "폐백 대행 업체 이용해보신 분 계세요",
                "양가 어른들이 원하셔서 알아보는 중이에요.");
        addFree(boards, "업체정보", "청첩장 인쇄 업체 추천해요",
                "모바일 청첩장이랑 같이 제작해주는 곳 찾고 있어요.");
        addFree(boards, "업체정보", "예물 브랜드별 특징 정리해봤어요",
                "알아보면서 정리한 내용 공유드려요.");
        addFree(boards, "업체정보", "혼주 한복 대여 업체 궁금해요",
                "어머니들 한복 대여하려는데 잘 아시는 분?");
        addFree(boards, "업체정보", "스튜디오 컨셉 촬영 후기",
                "야외 촬영이랑 실내 촬영 둘 다 해봤는데 비교 후기 남겨요.");
        addFree(boards, "업체정보", "예식장 주차 잘 되는 곳 정보 나눠요",
                "하객분들 배려해서 주차 넉넉한 곳 위주로 알아봤어요.");
        addFree(boards, "업체정보", "웨딩 플래너 이용 후기",
                "직접 준비 vs 플래너 이용, 장단점 느낀 점 공유합니다.");
        addFree(boards, "업체정보", "답례품 제작 업체 후기",
                "실용성 위주로 골랐던 경험 나눠드려요.");

        addFree(boards, "잡담", "결혼 앞두니 잠이 안 와요",
                "다들 이런 기분이셨나요 ㅠㅠ");
        addFree(boards, "잡담", "예비신랑이 무심해서 서운해요",
                "준비는 저 혼자 다 하는 느낌인데 다들 비슷하신가요?");
        addFree(boards, "잡담", "결혼 준비하면서 살 빠진 분 계세요",
                "스트레스로 입맛이 없네요.");
        addFree(boards, "잡담", "상견례 자리 너무 떨렸어요",
                "무사히 끝났는데 기억이 하나도 안 나요 ㅋㅋ");
        addFree(boards, "잡담", "웨딩드레스 입어보고 눈물 났어요",
                "실감이 나서 그런지 감정이 벅차오르더라고요.");
        addFree(boards, "잡담", "결혼식 D-100 기념 소소한 자랑",
                "드디어 100일 남았네요, 다들 화이팅이에요.");
        addFree(boards, "잡담", "친구 결혼식 다녀왔는데 부럽더라고요",
                "저도 얼른 준비 끝내고 싶어요.");
        addFree(boards, "잡담", "예비 시댁 어른들 뵙고 왔어요",
                "다들 상견례 전 긴장 어떻게 푸셨나요?");
        addFree(boards, "잡담", "결혼 준비 하다 보니 남친이랑 더 애틋해졌어요",
                "힘든 만큼 서로 의지하게 되네요.");
        addFree(boards, "잡담", "웨딩홀 답사 다녀온 후기 잡담",
                "발이 너무 아팠지만 재밌었어요.");
        addFree(boards, "잡담", "결혼식 앞두고 다이어트 시작했어요",
                "같이 하실 분 계신가요?");
        addFree(boards, "잡담", "부모님이 더 좋아하시는 것 같아요",
                "저보다 부모님이 더 신나 보이셔서 웃겼어요.");

        addFree(boards, "꿀팁", "청첩장 모임 준비 꿀팁 공유해요",
                "미리 준비해두면 편한 것들 정리했어요.");
        addFree(boards, "꿀팁", "웨딩홀 계약 전 꼭 확인해야 할 것들",
                "계약서 조항 꼼꼼히 보라는 말, 진짜 중요해요.");
        addFree(boards, "꿀팁", "스드메 계약 시 체크리스트",
                "수정 횟수, 원본 제공 여부 꼭 확인하세요.");
        addFree(boards, "꿀팁", "예산 관리 앱으로 지출 정리하는 팁",
                "카테고리별로 나눠서 관리하니 훨씬 편해졌어요.");
        addFree(boards, "꿀팁", "혼주 선물 고를 때 팁",
                "실용적인 것 위주로 골랐더니 반응이 좋았어요.");
        addFree(boards, "꿀팁", "결혼식 당일 준비물 체크리스트",
                "놓치기 쉬운 것들 정리해봤어요.");
        addFree(boards, "꿀팁", "신혼집 계약 전 확인사항 정리",
                "등기부등본 꼭 확인하세요.");
        addFree(boards, "꿀팁", "예비부부 통장 관리 팁",
                "공동 통장 만들어서 관리하니 편하더라고요.");
        addFree(boards, "꿀팁", "결혼 준비 시기별 할 일 정리",
                "6개월 전부터 순서대로 정리해봤어요.");
        addFree(boards, "꿀팁", "청첩장 문구 참고할 만한 사이트 공유",
                "여러 스타일 참고하기 좋았어요.");
        addFree(boards, "꿀팁", "웨딩 촬영 포즈 꿀팁",
                "어색하지 않게 나오는 팁 몇 가지 나눠요.");
        addFree(boards, "꿀팁", "혼인신고 준비서류 미리 체크하세요",
                "미리 알아두면 당일 편해요.");

        addReview(boards, "홀", "강남 그레이스 웨딩홀 후기 - 가성비 끝판왕",
                "가격 대비 만족도 매우 높음. 수용인원 300명, 주차 편리. 식사 퀄리티도 기대 이상이었어요.", 5);
        addReview(boards, "홀", "잠실 롯데호텔 웨딩홀 이용 후기",
                "시설은 최고인데 가격대가 있는 편이에요. 예산 넉넉하신 분들께 추천.", 4);
        addReview(boards, "홀", "여의도 한강뷰 웨딩홀 다녀왔어요",
                "사진 정말 예쁘게 나왔어요. 강력 추천합니다.", 5);
        addReview(boards, "홀", "컨벤션 이용 후기 - 아쉬운 점도 있었어요",
                "위치는 좋은데 대기 공간이 좁았어요.", 3);
        addReview(boards, "홀", "야외 정원 웨딩홀 후기",
                "날씨만 좋으면 최고의 선택인 것 같아요.", 4);
        addReview(boards, "홀", "소규모 웨딩홀 대관 후기",
                "가족적인 분위기로 진행하기 좋았어요.", 5);
        addReview(boards, "홀", "특약 조항 꼼꼼히 확인 안 해서 후회한 후기",
                "계약 전 반드시 특약사항 확인하세요.", 2);
        addReview(boards, "홀", "역삼 웨딩홀 식사 퀄리티 후기",
                "하객분들 반응이 좋았어요.", 4);
        addReview(boards, "홀", "평일 웨딩 할인 받고 진행한 후기",
                "비용 많이 아꼈어요, 추천합니다.", 5);
        addReview(boards, "홀", "주차 공간 부족했던 웨딩홀 후기",
                "하객분들이 불편해하셔서 아쉬웠어요.", 3);

        addReview(boards, "스드메", "스드메 패키지 가격 공개합니다 - 총 280만원",
                "수정 2회 포함, 원본 파일 제공되는 조건이었어요.", 5);
        addReview(boards, "스드메", "드레스샵 투어 후기 - 5곳 비교",
                "예약 필수, 평일 방문 추천드려요.", 4);
        addReview(boards, "스드메", "스튜디오 촬영 아쉬웠던 점",
                "원본 제공이 안 돼서 당황했어요.", 3);
        addReview(boards, "스드메", "메이크업 리허설 후기",
                "원하는 스타일 미리 상담하니 만족스러웠어요.", 4);
        addReview(boards, "스드메", "드레스 피팅 꿀팁 후기",
                "미리 붓기 관리하고 가시길 추천해요.", 4);
        addReview(boards, "스드메", "스드메 업체 응대 아쉬웠던 후기",
                "연락이 잘 안 돼서 답답했어요.", 2);
        addReview(boards, "스드메", "본식 스냅 작가님 실력 후기",
                "자연스러운 컷 정말 많이 건졌어요.", 5);
        addReview(boards, "스드메", "한복 스냅 촬영 후기",
                "색감이 예쁘게 나왔어요.", 4);
        addReview(boards, "스드메", "야외 컨셉 촬영 후기 - 날씨 변수 있었어요",
                "우천으로 실내로 급변경했어요.", 3);
        addReview(boards, "스드메", "가성비 스드메 업체 후기",
                "합리적인 가격에 만족스러운 결과물이었어요.", 5);

        addReview(boards, "예복", "맞춤 예복 후기 - 핏이 다르긴 하네요",
                "기성복보다 확실히 딱 떨어져요.", 4);
        addReview(boards, "예복", "예복 렌탈 후기",
                "가격은 저렴한데 사이즈 선택지가 적었어요.", 3);
        addReview(boards, "예복", "턱시도 브랜드 비교 후기",
                "여러 브랜드 입어보고 결정했어요.", 5);
        addReview(boards, "예복", "예복 맞춤 vs 기성복 가격 차이 후기",
                "맞춤 80~150만원, 기성 40~70만원 정도였어요.", 4);
        addReview(boards, "예복", "예복 수선 늦어져서 마음 졸인 후기",
                "여유있게 맡기시길 추천드려요.", 2);
        addReview(boards, "예복", "신랑 예복 코디 후기",
                "스타일리스트 추천 조합이 만족스러웠어요.", 5);
        addReview(boards, "예복", "예복 대여 기간 확인 꼭 하세요",
                "저는 하루 차이로 추가 비용 냈어요.", 3);
        addReview(boards, "예복", "체형 고려한 예복 선택 후기",
                "체형 커버 잘 되는 핏 찾는 게 중요하더라고요.", 4);
        addReview(boards, "예복", "혼주 예복 맞춤 후기",
                "부모님도 만족하셨어요.", 5);
        addReview(boards, "예복", "예복 피팅 3회 진행 후기",
                "여유있게 일정 잡으시길 추천해요.", 4);

        addReview(boards, "예물", "예물 반지 브랜드별 가격 비교해봤어요",
                "국내 브랜드도 퀄리티 좋더라고요.", 5);
        addReview(boards, "예물", "예물 시계 선택 후기",
                "실용성 고려해서 골랐어요.", 4);
        addReview(boards, "예물", "예물 예산 초과했던 후기",
                "예상보다 비용이 커져서 당황했어요.", 3);
        addReview(boards, "예물", "커플링 각인 문구 고민 후기",
                "심플한 문구로 결정했어요.", 4);
        addReview(boards, "예물", "예물 상담 받으러 다닌 후기",
                "여러 곳 비교해보길 잘했어요.", 4);
        addReview(boards, "예물", "예물 배송 지연됐던 후기",
                "여유있게 주문하세요.", 2);
        addReview(boards, "예물", "다이아 등급 공부하고 고른 후기",
                "미리 공부하니 상담이 수월했어요.", 5);
        addReview(boards, "예물", "예물 리폼 후기",
                "나중에 리폼 가능한 디자인으로 골랐어요.", 4);
        addReview(boards, "예물", "예물 가격 흥정 후기",
                "매장마다 차이가 있어서 비교가 필요해요.", 3);
        addReview(boards, "예물", "심플한 예물로 만족한 후기",
                "과하지 않게 준비해서 좋았어요.", 5);

        addReview(boards, "기타", "답례품 뭐가 좋을까요 - 돌잔치랑 다른 점",
                "실용적인 소품이 인기 높음. 1인당 1만원 내외가 적절했어요.", 5);
        addReview(boards, "기타", "청첩장 인쇄 업체 후기",
                "퀄리티 대비 가격 만족스러웠어요.", 4);
        addReview(boards, "기타", "혼인신고 절차 후기",
                "생각보다 서류 준비가 까다로웠어요.", 3);
        addReview(boards, "기타", "신혼여행 패키지 후기",
                "가성비 좋은 곳으로 잘 골랐어요.", 5);
        addReview(boards, "기타", "웨딩카 대여 후기",
                "클래식카로 예약했는데 사진이 예뻤어요.", 4);
        addReview(boards, "기타", "답례품 배송 지연 후기",
                "여유있게 주문하시길 추천드려요.", 2);
        addReview(boards, "기타", "모바일 청첩장 제작 후기",
                "사진 배치가 예쁘게 나왔어요.", 5);
        addReview(boards, "기타", "결혼식 사회 대행 후기",
                "전문 사회자라 진행이 매끄러웠어요.", 4);
        addReview(boards, "기타", "혼주 인사말 준비 후기",
                "미리 준비 안 해서 당황하셨더라고요.", 3);
        addReview(boards, "기타", "결혼식 축가 섭외 후기",
                "지인분이 불러주셔서 더 감동이었어요.", 5);

        boardRepository.saveAll(boards);

        log.info("게시판 더미데이터 로딩 완료 (" + boards.size() + "건)");
    }

    private void addFree(List<Board> boards, String category, String title, String content) {
        add(boards, "FREE", category, title, content, null);
    }

    private void addReview(List<Board> boards, String category, String title, String content, int rating) {
        add(boards, "REVIEW", category, title, content, rating);
    }

    private void add(List<Board> boards, String boardType, String category, String title, String content, Integer rating) {

        int i = idx++;

        int viewCount = 80 + (i * 37) % 900;
        int likeCount = 5 + (i * 7) % 90;
        long daysAgo = i % 45;
        long hoursAgo = (i * 3) % 24;

        boards.add(Board.builder()
                .memberEmail(DUMMY_EMAIL)
                .nickname(NICKNAMES[i % NICKNAMES.length])
                .boardType(boardType)
                .category(category)
                .title(title)
                .content(content)
                .rating(rating)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .regDate(LocalDateTime.now().minusDays(daysAgo).minusHours(hoursAgo))
                .build());
    }

}
