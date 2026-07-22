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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 피드백: 상단 "조합 히스토리 배지"에 확정/해제/다시찾기/제외 같은 사이드패널 버튼 액션까지
// 새 배지로 쌓여서 뭘 눌렀는지 헷갈린다 - 자유발화(다듬기 채팅)만 배지가 되어야 한다.
// 그래서 applySlotAction은 더 이상 새 히스토리 턴을 만들지 않고, 그 대신 마지막 턴의 스냅샷을
// 지금 상태로 갱신한다(AiPlanSessionSupport.refreshLatestHistorySnapshot) - 안 그러면 나중에
// 그 배지를 다시 봤을 때 방금 한 확정/제외가 사라져버리기 때문. 롤백 기본값이라 테스트 끝나면
// DB에 안 남는다.
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
    public void testSlotActionsDoNotAddNewTurnButStillApply() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        assertEquals(1, historySize(sessionId), "초기 추천 직후엔 턴이 1개(0번)여야 함");

        Map<String, Object> excludeRequest = Map.of(
                "sessionId", sessionId, "category", "STUDIO", "action", "EXCLUDE");
        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excludeRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) slotBody.get("candidates")).get(0);

        assertEquals("EXCLUDED", combo.get("studioStatus"), "제외 액션 자체는 여전히 정상 반영돼야 함");
        assertNull(combo.get("studioCmno"), "제외되면 업체는 비어 있어야 함");
        assertEquals(1, historySize(sessionId),
                "사이드패널 버튼 액션(제외)은 새 배지(턴)를 만들면 안 됨");
    }

    @Test
    @Transactional
    public void testViewingLatestTurnAfterSlotActionShowsRefreshedStateNotStale() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> confirmRequest = Map.of(
                "sessionId", sessionId, "category", "HALL", "action", "CONFIRM");
        mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // 새 배지가 안 생겼으니 여전히 0번 턴 하나뿐 - 그 0번 배지를 다시 봐도 방금 한 확정이
        // 사라지면 안 된다(스냅샷을 갱신해두기 때문).
        assertEquals(1, historySize(sessionId));

        String viewResponse = mockMvc.perform(post("/api/aiplan/session/" + sessionId + "/turn/0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> viewBody = objectMapper.readValue(viewResponse, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) viewBody.get("candidates")).get(0);

        log.info("after CONFIRM + view turn 0: hallStatus={}", combo.get("hallStatus"));

        assertNotNull(combo.get("hallCmno"));
        assertEquals("CONFIRMED", combo.get("hallStatus"),
                "0번 배지를 다시 봐도 방금 확정한 상태가 유지돼야 함(스냅샷 갱신 덕분)");
    }

    private int historySize(Long sessionId) throws Exception {
        String response = mockMvc.perform(get("/api/aiplan/session/" + sessionId + "/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, List.class).size();
    }
}
