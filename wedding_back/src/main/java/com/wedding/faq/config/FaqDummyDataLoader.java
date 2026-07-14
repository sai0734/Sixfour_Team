package com.wedding.faq.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.wedding.faq.domain.Faq;
import com.wedding.faq.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 서버 부팅 시 한 번만 실행. 이미 데이터가 있으면 건너뜀(중복 삽입 방지)
@Component
@RequiredArgsConstructor
@Log4j2
public class FaqDummyDataLoader implements CommandLineRunner {

    private final FaqRepository faqRepository;

    @Override
    public void run(String... args) {

        if (faqRepository.count() > 0) {
            log.info("FAQ 더미데이터 이미 존재, 로딩 생략");
            return;
        }

        log.info("FAQ 더미데이터 35개 로딩 시작.........");

        List<Faq> faqs = List.of(

                // ===== 회원·로그인 (5) =====
                Faq.builder().category("회원·로그인").sortOrder(1)
                        .likeCount(45)
                        .question("회원가입은 어떻게 하나요?")
                        .answer("메인 화면 우측 상단의 '회원가입' 버튼을 눌러 이메일, 비밀번호, 닉네임을 입력하면 가입할 수 있어요. 가입 후 인증 이메일을 확인해야 로그인이 가능합니다.")
                        .build(),
                Faq.builder().category("회원·로그인").sortOrder(2)
                        .likeCount(92)
                        .question("비밀번호를 잊어버렸어요. 어떻게 재설정하나요?")
                        .answer("로그인 화면의 '비밀번호 찾기'를 눌러 가입한 이메일을 입력하면, 비밀번호 재설정 링크를 보내드려요.")
                        .build(),
                Faq.builder().category("회원·로그인").sortOrder(3)
                        .likeCount(78)
                        .question("로그인이 계속 안 돼요.")
                        .answer("이메일/비밀번호를 다시 확인해주세요. 5회 연속 실패하면 보안을 위해 계정이 잠기며, 이 경우 비밀번호 재설정 절차를 거쳐야 다시 로그인할 수 있어요.")
                        .build(),
                Faq.builder().category("회원·로그인").sortOrder(4)
                        .likeCount(38)
                        .question("카카오 계정으로도 로그인할 수 있나요?")
                        .answer("네, 로그인 화면 하단의 카카오 로그인 버튼을 이용하면 별도 회원가입 없이 바로 이용하실 수 있어요.")
                        .build(),
                Faq.builder().category("회원·로그인").sortOrder(5)
                        .likeCount(29)
                        .question("이메일 인증 메일이 오지 않아요.")
                        .answer("스팸함을 먼저 확인해주세요. 그래도 안 보이면 회원가입 화면에서 인증 메일을 다시 요청할 수 있어요.")
                        .build(),

                // ===== 예산 (5) =====
                Faq.builder().category("예산").sortOrder(1)
                        .likeCount(56)
                        .question("예산 관리 기능은 어떻게 사용하나요?")
                        .answer("준비관리 > 예산 관리에서 카테고리별(홀/스드메/예복/예물/기타)로 계획 예산과 실지출을 각각 등록하면, 잔여 예산이 자동으로 계산돼요.")
                        .build(),
                Faq.builder().category("예산").sortOrder(2)
                        .likeCount(24)
                        .question("실지출 금액은 언제 입력해야 하나요?")
                        .answer("항목 등록 시에는 0으로 시작하고, 실제로 결제가 완료된 후에 실지출 금액을 수정해서 입력하시면 돼요.")
                        .build(),
                Faq.builder().category("예산").sortOrder(3)
                        .likeCount(31)
                        .question("예산을 초과하면 알림이 오나요?")
                        .answer("현재는 예산 관리 화면에서 초과된 항목이 빨간색으로 강조 표시돼요. 실시간 알림 기능은 준비 중이에요.")
                        .build(),
                Faq.builder().category("예산").sortOrder(4)
                        .likeCount(18)
                        .question("예산 항목 순서는 어떻게 바꾸나요?")
                        .answer("항목 수정에서 순서 번호를 원하는 숫자로 바꾸면, 기존에 그 번호를 쓰던 항목과 자동으로 순서가 서로 바뀌어요.")
                        .build(),
                Faq.builder().category("예산").sortOrder(5)
                        .likeCount(41)
                        .question("총 예산은 어디서 설정하나요?")
                        .answer("마이페이지의 웨딩플랜에서 총 예산을 등록할 수 있고, 예산 관리 화면의 항목별 합계와 별도로 관리돼요.")
                        .build(),

                // ===== 업체·예약 (5) =====
                Faq.builder().category("업체·예약").sortOrder(1)
                        .likeCount(67)
                        .question("업체는 어떻게 예약하나요?")
                        .answer("업체탐색에서 원하는 업체를 선택한 뒤 예약하기 버튼을 누르면, 마이페이지 > 예약 현황에서 예약 상태를 확인할 수 있어요.")
                        .build(),
                Faq.builder().category("업체·예약").sortOrder(2)
                        .likeCount(52)
                        .question("계약금과 잔금은 어떻게 구분해서 관리하나요?")
                        .answer("준비관리 > 납부 관리에서 계약금/잔금을 각각 별도 항목으로 등록하고, 납부 기한과 상태(대기/완료/취소)를 관리할 수 있어요.")
                        .build(),
                Faq.builder().category("업체·예약").sortOrder(3)
                        .likeCount(35)
                        .question("예약을 취소하고 싶어요.")
                        .answer("마이페이지 > 예약 현황에서 해당 예약을 수정해 상태를 '취소'로 변경하거나 삭제할 수 있어요.")
                        .build(),
                Faq.builder().category("업체·예약").sortOrder(4)
                        .likeCount(44)
                        .question("납부 기한을 놓치면 어떻게 되나요?")
                        .answer("납부 관리 화면에서 기한이 지난 항목은 '기한 초과'로 빨갛게 표시돼요. 실제 계약 불이익 여부는 업체와 직접 확인이 필요해요.")
                        .build(),
                Faq.builder().category("업체·예약").sortOrder(5)
                        .likeCount(22)
                        .question("찜한 업체는 어디서 볼 수 있나요?")
                        .answer("마이페이지 > 찜 목록에서 그동안 찜해둔 업체를 모아볼 수 있어요.")
                        .build(),

                // ===== 일정 (5) =====
                Faq.builder().category("일정").sortOrder(1)
                        .likeCount(58)
                        .question("체크리스트의 단계(1/2/3단계)는 무슨 의미인가요?")
                        .answer("1단계는 기본 계획, 2단계는 업체 계약, 3단계는 청첩장·답례품 준비를 뜻해요. 결혼 준비 흐름 순서대로 나뉘어 있어요.")
                        .build(),
                Faq.builder().category("일정").sortOrder(2)
                        .likeCount(20)
                        .question("체크리스트 항목 순서는 어떻게 바꾸나요?")
                        .answer("항목 수정에서 원하는 순서 번호를 입력하면, 같은 단계 내에서 기존 항목과 순서가 서로 바뀌어요.")
                        .build(),
                Faq.builder().category("일정").sortOrder(3)
                        .likeCount(63)
                        .question("D-day는 어떤 기준으로 계산되나요?")
                        .answer("마이페이지 웨딩플랜에 등록한 예식일을 기준으로 자동 계산돼요. D-day 관리 화면에서 체크리스트 마감일, 납부 기한과 함께 한 번에 볼 수 있어요.")
                        .build(),
                Faq.builder().category("일정").sortOrder(4)
                        .likeCount(37)
                        .question("예식일은 어디서 등록하나요?")
                        .answer("마이페이지 > 플랜 탭에서 예식일, 예식장, 총 예산 등 웨딩 기본 정보를 등록할 수 있어요.")
                        .build(),
                Faq.builder().category("일정").sortOrder(5)
                        .likeCount(19)
                        .question("마감일이 지난 체크리스트 항목은 어떻게 표시되나요?")
                        .answer("완료 처리하지 않은 채 마감일이 지나도 별도 경고 표시는 없고, D-day 관리 화면의 '지난 일정'에서 확인할 수 있어요.")
                        .build(),

                // ===== 답례품 쇼핑몰 (5) =====
                Faq.builder().category("답례품 쇼핑몰").sortOrder(1)
                        .likeCount(48)
                        .question("주문 내역은 어디서 확인하나요?")
                        .answer("답례품 쇼핑몰 내 마이페이지(주문 조회) 메뉴에서 확인하실 수 있어요.")
                        .build(),
                Faq.builder().category("답례품 쇼핑몰").sortOrder(2)
                        .likeCount(33)
                        .question("상품에 후기(댓글)는 어떻게 남기나요?")
                        .answer("구매한 상품 상세 페이지 하단의 댓글 영역에 후기를 작성할 수 있어요. 별도 인증 절차 없이 로그인 후 바로 작성 가능해요.")
                        .build(),
                Faq.builder().category("답례품 쇼핑몰").sortOrder(3)
                        .likeCount(71)
                        .question("주문한 상품을 교환·환불하고 싶어요.")
                        .answer("상품 수령 후 7일 이내에 고객센터로 문의해주시면 교환·환불 절차를 안내해드려요.")
                        .build(),
                Faq.builder().category("답례품 쇼핑몰").sortOrder(4)
                        .likeCount(25)
                        .question("장바구니에 담은 상품은 얼마나 유지되나요?")
                        .answer("로그인 계정 기준으로 별도 삭제 전까지 계속 유지돼요.")
                        .build(),
                Faq.builder().category("답례품 쇼핑몰").sortOrder(5)
                        .likeCount(39)
                        .question("배송 조회는 어떻게 하나요?")
                        .answer("주문 조회 화면에서 각 주문 건의 배송 상태와 운송장 번호를 확인할 수 있어요.")
                        .build(),

                // ===== 커뮤니티 (5) =====
                Faq.builder().category("커뮤니티").sortOrder(1)
                        .likeCount(30)
                        .question("게시글은 어떻게 작성하나요?")
                        .answer("커뮤니티 > 자유게시판 또는 후기게시판에서 '+ 글쓰기' 버튼을 누르면 작성 창이 열려요.")
                        .build(),
                Faq.builder().category("커뮤니티").sortOrder(2)
                        .likeCount(27)
                        .question("게시글에 이미지를 첨부할 수 있나요?")
                        .answer("네, 이미지 여러 장을 한 번에 첨부할 수 있어요.")
                        .build(),
                Faq.builder().category("커뮤니티").sortOrder(3)
                        .likeCount(34)
                        .question("댓글에 답글(대댓글)도 달 수 있나요?")
                        .answer("네, 댓글 아래 '답글' 버튼을 누르면 해당 댓글에 대한 답글을 작성할 수 있어요.")
                        .build(),
                Faq.builder().category("커뮤니티").sortOrder(5)
                        .likeCount(15)
                        .question("부적절한 게시글은 어떻게 신고하나요?")
                        .answer("신고 기능은 현재 준비 중이에요. 우선 고객센터로 문의해주시면 빠르게 확인 후 조치할게요.")
                        .build(),

                // ===== 기타 (5) =====
                Faq.builder().category("기타").sortOrder(1)
                        .likeCount(60)
                        .question("서비스 이용료가 있나요?")
                        .answer("웨딩올인원의 기본 기능(체크리스트, 예산관리, 커뮤니티 등)은 무료로 이용하실 수 있어요.")
                        .build(),
                Faq.builder().category("기타").sortOrder(2)
                        .likeCount(42)
                        .question("입력한 개인정보는 어떻게 관리되나요?")
                        .answer("비밀번호는 암호화되어 저장되며, 개인정보는 서비스 제공 목적 외에는 사용되지 않아요.")
                        .build(),
                Faq.builder().category("기타").sortOrder(3)
                        .likeCount(28)
                        .question("회원 탈퇴는 어떻게 하나요?")
                        .answer("회원정보 수정 화면에서 회원 탈퇴를 진행할 수 있어요. 탈퇴 시 등록된 데이터는 복구가 어려우니 신중히 결정해주세요.")
                        .build(),
                Faq.builder().category("기타").sortOrder(4)
                        .likeCount(33)
                        .question("문의는 어디로 하면 되나요?")
                        .answer("이 FAQ에서 답을 찾지 못하셨다면 고객센터 이메일로 문의해주시면 빠르게 답변드릴게요.")
                        .build(),
                Faq.builder().category("기타").sortOrder(5)
                        .likeCount(21)
                        .question("모바일에서도 이용할 수 있나요?")
                        .answer("네, 모바일 브라우저에서도 반응형으로 이용하실 수 있어요.")
                        .build()
        );

        faqRepository.saveAll(faqs);

        log.info("FAQ 더미데이터 로딩 완료 (" + faqs.size() + "건)");
    }

}
