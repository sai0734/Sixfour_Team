package com.wedding.inquiry.repository;

import com.wedding.inquiry.domain.InquiryRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

public interface InquiryRoomRepository extends JpaRepository<InquiryRoom, Long> {

    // 회원 + 업체로 기존 방 찾기 (문의하기 클릭 시 "이미 있으면 재사용")
    @Query("SELECT r FROM InquiryRoom r WHERE r.memberEmail = :memberEmail AND r.cmno = :cmno")
    Optional<InquiryRoom> findByMemberEmailAndCmno(
            @Param("memberEmail") String memberEmail,
            @Param("cmno") Long cmno
    );

    // 매니저 화면: 해당 업체의 모든 문의방 (최근 대화순)
    @Query("SELECT r FROM InquiryRoom r WHERE r.cmno = :cmno ORDER BY r.lastMessageAt DESC")
    List<InquiryRoom> findByCmnoOrderByLastMessageAtDesc(@Param("cmno") Long cmno);

    // 회원 화면: 내가 연 모든 문의방 (최근 대화순) — 플로팅 버튼 안읽음 뱃지 폴링용
    @Query("SELECT r FROM InquiryRoom r WHERE r.memberEmail = :memberEmail ORDER BY r.lastMessageAt DESC")
    List<InquiryRoom> findByMemberEmailOrderByLastMessageAtDesc(@Param("memberEmail") String memberEmail);

    // 관리자 대시보드용 집계
    long countByStatus(com.wedding.inquiry.domain.InquiryRoomStatus status);

}
