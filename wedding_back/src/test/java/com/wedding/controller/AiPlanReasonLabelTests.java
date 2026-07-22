package com.wedding.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 피드백 2단계: 각 업체 카드 아래 초록색 "왜 이 업체를 골랐는지" 설명이 처음 추천/다듬기
// 직후에만 반짝 떴다가 사라지는 게 문제였음(세션에 저장 안 되는 값이라 새로고침/확정 등
// 아무 액션이나 하면 없어짐) - AiPlanCandidateBuilder.pickReason()으로 예산/지역 매칭 여부를
// 매번 다시 계산해서 붙이도록 바꿔서, 최초 추천이든 세션 복원이든 항상 같은 방식으로 뜨는지
// 확인한다. 이제 "방금 확정/다시찾기 했다"가 아니라 "왜 이 업체가 조건에 맞는지"를 보여주는
// 용도라 CONFIRMED/PENDING 둘 다 값이 있어야 하고, EXCLUDED만 없어야 한다.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanReasonLabelTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testPendingSlotsHaveReasonLabel() throws Exception {

        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) body.get("candidates")).get(0);

        log.info("initial combo reasons: hall={}, studio={}, dress={}, makeup={}",
                combo.get("hallReason"), combo.get("studioReason"),
                combo.get("dressReason"), combo.get("makeupReason"));

        assertNotNull(combo.get("hallReason"), "왜 이 업체를 골랐는지는 처음 추천부터 있어야 함");
        assertNotNull(combo.get("studioReason"));
        assertNotNull(combo.get("dressReason"));
        assertNotNull(combo.get("makeupReason"));
    }

    @Test
    @Transactional
    public void testReasonLabelSurvivesSessionRestoreAndConfirm() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        // 새로고침 복원(getSession)에서도 사라지지 않아야 함 - 이게 이번에 고친 핵심.
        String sessionResponse = mockMvc.perform(get("/api/aiplan/session/" + sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> sessionBody = objectMapper.readValue(sessionResponse, Map.class);
        Map<?, ?> restoredCombo = (Map<?, ?>) ((List<?>) sessionBody.get("candidates")).get(0);
        assertNotNull(restoredCombo.get("hallReason"), "새로고침 복원 후에도 이유 설명이 남아있어야 함");

        // 확정한 뒤에도 여전히 있어야 함 (더 이상 "확정하신 곳이에요"라는 고정 문구가 아니라
        // 같은 예산/지역 기반 이유가 계속 붙음)
        Map<String, Object> confirmRequest = Map.of(
                "sessionId", sessionId, "category", "HALL", "action", "CONFIRM");
        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        Map<?, ?> confirmedCombo = (Map<?, ?>) ((List<?>) slotBody.get("candidates")).get(0);
        assertNotNull(confirmedCombo.get("hallReason"), "확정 후에도 이유 설명이 남아있어야 함");

        // 제외된 카테고리는 업체 자체가 없으니 이유도 없어야 함
        Map<String, Object> excludeRequest = Map.of(
                "sessionId", sessionId, "category", "STUDIO", "action", "EXCLUDE");
        String excludeResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excludeRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> excludeBody = objectMapper.readValue(excludeResponse, Map.class);
        Map<?, ?> excludedCombo = (Map<?, ?>) ((List<?>) excludeBody.get("candidates")).get(0);
        assertNull(excludedCombo.get("studioReason"), "제외된 카테고리는 이유도 없어야 함");
    }
}
