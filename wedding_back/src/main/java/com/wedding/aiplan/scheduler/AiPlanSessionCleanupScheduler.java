package com.wedding.aiplan.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.repository.AiPlanSessionHistoryRepository;
import com.wedding.aiplan.repository.AiPlanSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AI 웨딩플랜은 추천을 새로 받을 때마다 세션이 하나씩 쌓이는 구조라(로그인 여부 무관), 방치된
// 세션이 계속 순증하지 않도록 매일 자정에 정리한다. "마이페이지에 담기"(appliedToPlan=true)를
// 누른 세션은 로그인 여부와 상관없이 절대 지우지 않고, 담지 않은 세션만 마지막 활동
// (updatedAt) 기준 session-ttl-days가 지나면 삭제한다. AiPlanSessionHistory는 sessionId를
// FK가 아니라 값으로만 들고 있어서 세션을 지워도 자동으로 안 지워지므로 먼저 지운다.
@Component
@Log4j2
@RequiredArgsConstructor
public class AiPlanSessionCleanupScheduler {

    private final AiPlanSessionRepository sessionRepository;
    private final AiPlanSessionHistoryRepository historyRepository;

    @Value("${com.wedding.aiplan.session-ttl-days}")
    private int sessionTtlDays;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupUnappliedSessions() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(sessionTtlDays);
        List<AiPlanSession> expired = sessionRepository.findByAppliedToPlanFalseAndUpdatedAtBefore(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("담기 안 하고 " + sessionTtlDays + "일 이상 방치되어 자동 삭제되는 AI 웨딩플랜 세션 수: "
                + expired.size());

        List<Long> sessionIds = expired.stream().map(AiPlanSession::getSessionId).toList();
        historyRepository.deleteBySessionIdIn(sessionIds);
        sessionRepository.deleteAll(expired);
    }

}
