package com.wedding.aiplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.aiplan.domain.AiPlanSession;

public interface AiPlanSessionRepository extends JpaRepository<AiPlanSession, Long> {

    // 로그인한 회원의 세션 목록 (최근 것부터, 마이페이지 등에서 "이어서 하기" 노출용)
    List<AiPlanSession> findByMemberEmailOrderByUpdatedAtDesc(String memberEmail);

}
