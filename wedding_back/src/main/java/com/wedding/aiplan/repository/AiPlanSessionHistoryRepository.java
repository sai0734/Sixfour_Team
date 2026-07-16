package com.wedding.aiplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.aiplan.domain.AiPlanSessionHistory;

public interface AiPlanSessionHistoryRepository extends JpaRepository<AiPlanSessionHistory, Long> {

    // 되돌리기 목록/복원용 - 턴 순서대로 조회
    List<AiPlanSessionHistory> findBySessionIdOrderByTurnNoAsc(Long sessionId);

    // 가장 최근 턴 하나만 필요할 때 (예: "직전 상태로 되돌리기")
    AiPlanSessionHistory findTopBySessionIdOrderByTurnNoDesc(Long sessionId);

}
