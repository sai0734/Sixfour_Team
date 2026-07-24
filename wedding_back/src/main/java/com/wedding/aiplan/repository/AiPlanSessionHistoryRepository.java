package com.wedding.aiplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.aiplan.domain.AiPlanSessionHistory;

public interface AiPlanSessionHistoryRepository extends JpaRepository<AiPlanSessionHistory, Long> {

    // 되돌리기 목록/복원용 - 턴 순서대로 조회
    List<AiPlanSessionHistory> findBySessionIdOrderByTurnNoAsc(Long sessionId);

    // DB 정리 배치용 - sessionId가 FK로 안 묶여있어서(값으로만 저장) 세션을 지워도 히스토리는
    // 자동으로 안 지워진다. 세션 지우기 직전에 이걸로 먼저 지운다.
    void deleteBySessionIdIn(List<Long> sessionIds);

}
