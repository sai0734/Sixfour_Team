package com.wedding.aiplan.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.aiplan.domain.AiPlanSession;

public interface AiPlanSessionRepository extends JpaRepository<AiPlanSession, Long> {

    // 로그인한 회원의 세션 목록 (최근 것부터, 마이페이지 등에서 "이어서 하기" 노출용)
    List<AiPlanSession> findByMemberEmailOrderByUpdatedAtDesc(String memberEmail);

    // DB 정리 배치(AiPlanSessionCleanupScheduler)용 - 로그인 여부와 무관하게 "마이페이지에
    // 담기"를 안 눌렀고(appliedToPlan=false) 마지막 활동이 cutoff보다 오래된 세션.
    List<AiPlanSession> findByAppliedToPlanFalseAndUpdatedAtBefore(LocalDateTime cutoff);

}
