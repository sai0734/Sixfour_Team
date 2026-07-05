package com.wedding.product.service;

import com.wedding.product.dto.ReviewDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ReviewService {

    // 상품 리뷰 목록 조회 (답변 포함, 계층 구조)
    List<ReviewDTO> listByProduct(Long pno, String currentMemberEmail);

    // 리뷰 작성 (구매 이력 있는 회원만 가능)
    Long register(String memberEmail, Long pno, Integer rating, String content, List<String> uploadFileNames);

    // 리뷰 수정 (본인만 가능)
    void modify(String memberEmail, Long pno, Long rno, Integer rating, String content, List<String> uploadFileNames);

    // 관리자 답변 등록
    Long reply(String adminEmail, Long pno, Long parentRno, String content);

    // 관리자 답변 수정
    void modifyReply(Long pno, Long rno, String content);

    // 리뷰 삭제 (본인 또는 관리자만 가능)
    void remove(String memberEmail, boolean isAdmin, Long pno, Long rno);

    // 리뷰 작성 자격 확인 (구매 이력 없으면 예외)
    void checkReviewEligibility(String memberEmail, Long pno);

}