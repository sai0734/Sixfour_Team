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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 결과 화면 상단 "조합 히스토리 배지" 기능 - 0번 턴(첫 추천 조합) 배지를 누르면 그 시점 조합을
// 그대로 불러와야 하고(EXCLUDE로 스튜디오를 뺐어도 0번을 보면 다시 있어야 함), "보기"만 한
// 것이므로 히스토리 턴 개수 자체는 늘어나면 안 된다. 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanViewTurnTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testViewTurnRestoresSnapshotWithoutAddingNewTurn() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        List<?> initialCandidates = (List<?>) detailBody.get("candidates");
        Map<?, ?> initialCombo = (Map<?, ?>) initialCandidates.get(0);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();
        Number initialStudioCmno = (Number) initialCombo.get("studioCmno");
        assertNotNull(initialStudioCmno, "초기 추천엔 스튜디오가 있어야 함");

        // 1번 턴: 스튜디오 제외
        Map<String, Object> excludeRequest = Map.of(
                "sessionId", sessionId, "category", "STUDIO", "action", "EXCLUDE");
        mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excludeRequest)))
                .andExpect(status().isOk());

        List<?> historyAfterExclude = objectMapper.readValue(
                mockMvc.perform(get("/api/aiplan/session/" + sessionId + "/history"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                List.class);
        assertEquals(2, historyAfterExclude.size(), "초기(0번) + 제외(1번) = 턴 2개여야 함");

        // 0번 턴("첫 추천 조합") 배지를 눌러 그 시점으로 이동
        String viewResponse = mockMvc.perform(post("/api/aiplan/session/" + sessionId + "/turn/0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> viewBody = objectMapper.readValue(viewResponse, Map.class);
        List<?> viewCandidates = (List<?>) viewBody.get("candidates");
        Map<?, ?> viewedCombo = (Map<?, ?>) viewCandidates.get(0);

        log.info("after view turn 0: studioStatus={}, studioCmno={}",
                viewedCombo.get("studioStatus"), viewedCombo.get("studioCmno"));

        assertEquals(initialStudioCmno.longValue(), ((Number) viewedCombo.get("studioCmno")).longValue(),
                "0번 턴을 보면 제외 전 스튜디오가 다시 보여야 함");
        assertEquals("PENDING", viewedCombo.get("studioStatus"), "0번 턴에서는 아직 제외 안 된 상태여야 함");

        List<?> historyAfterView = objectMapper.readValue(
                mockMvc.perform(get("/api/aiplan/session/" + sessionId + "/history"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                List.class);
        assertEquals(2, historyAfterView.size(), "턴을 보기만 한 것으로는 히스토리 개수가 늘면 안 됨");
    }
}
