package com.wedding.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.repository.AiPlanSessionRepository;
import com.wedding.aiplan.scheduler.AiPlanSessionCleanupScheduler;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DB 정리 배치 검증: (1) 담기 안 하고 30일 넘게 방치된 세션은 삭제, (2) 최근에 만든 세션은
// 방치 기준(updatedAt)에 안 걸려서 유지, (3) 30일 넘었어도 담기(appliedToPlan=true)한
// 세션은 로그인 여부와 무관하게 유지. 실제 스케줄러 빈을 직접 호출해서 cron 대기 없이 검증.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanSessionCleanupSchedulerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiPlanSessionRepository sessionRepository;

    @Autowired
    private AiPlanSessionCleanupScheduler cleanupScheduler;

    @Test
    @Transactional
    public void testCleanupDeletesOnlyStaleUnappliedSessions() throws Exception {

        Long staleUnappliedId = createSession();
        Long freshUnappliedId = createSession();
        Long staleAppliedId = createSession();

        LocalDateTime old = LocalDateTime.now().minusDays(31);
        backdate(staleUnappliedId, old);
        backdate(staleAppliedId, old);

        AiPlanSession staleApplied = sessionRepository.findById(staleAppliedId).orElseThrow();
        staleApplied.changeAppliedToPlan(true);
        sessionRepository.saveAndFlush(staleApplied);

        cleanupScheduler.cleanupUnappliedSessions();

        log.info("staleUnapplied exists={}, freshUnapplied exists={}, staleApplied exists={}",
                sessionRepository.existsById(staleUnappliedId),
                sessionRepository.existsById(freshUnappliedId),
                sessionRepository.existsById(staleAppliedId));

        assertFalse(sessionRepository.existsById(staleUnappliedId),
                "담기 안 하고 30일 넘게 방치된 세션은 삭제돼야 함");
        assertTrue(sessionRepository.existsById(freshUnappliedId),
                "방금 만든 세션은 아직 방치 기준에 안 걸려서 남아있어야 함");
        assertTrue(sessionRepository.existsById(staleAppliedId),
                "담기(appliedToPlan=true)한 세션은 30일 넘었어도 삭제되면 안 됨");
    }

    private Long createSession() throws Exception {
        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        return ((Number) body.get("sessionId")).longValue();
    }

    private void backdate(Long sessionId, LocalDateTime updatedAt) {
        AiPlanSession session = sessionRepository.findById(sessionId).orElseThrow();
        ReflectionTestUtils.setField(session, "updatedAt", updatedAt);
        sessionRepository.saveAndFlush(session);
    }
}
