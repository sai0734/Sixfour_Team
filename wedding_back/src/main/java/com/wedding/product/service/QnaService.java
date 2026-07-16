package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.dto.AdminQnaListDTO;
import com.wedding.product.dto.QnaDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface QnaService {

    // 상품 질문 목록 조회 (답변 포함)
    List<QnaDTO> listByProduct(Long pno, String currentMemberEmail);

    // 관리자용 - 답변 없는 질문 목록 (상품 무관, 전체)
    PageResponseDTO<AdminQnaListDTO> listUnanswered(PageRequestDTO requestDTO);

    // 질문 등록 (로그인 회원 누구나 가능)
    Long register(String memberEmail, Long pno, String content);

    // 질문 수정 (본인만 가능)
    void modify(String memberEmail, Long pno, Long qno, String content);

    // 관리자 답변 등록
    Long reply(String adminEmail, Long pno, Long parentQno, String content);

    // 관리자 답변 수정
    void modifyReply(Long pno, Long qno, String content);

    // 질문/답변 삭제 (본인 또는 관리자만 가능)
    void remove(String memberEmail, boolean isAdmin, Long pno, Long qno);

}