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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: "요청하신 대로 다시 골랐어요"가 각 업체 카드 아래에 항상 붙어있었음 - 방금
// 다시찾기를 했을 때만 떠야 하는데, 첫 추천 직후/새로고침 복원/조합 히스토리 배지로 예전 턴을
// 봤을 때도 떠 있었음. 원인은 SlotView.reasonLabel()이 "방금 다시 찾았는지"가 아니라 그냥
// PENDING 상태 전체에 이 문구를 붙였기 때문 - PENDING이면 무조건 null이어야 하고, CONFIRMED일
// 때만 문구가 있어야 한다. 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
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
    public void testPendingSlotsHaveNoReasonLabel() throws Exception {

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

        assertNull(combo.get("hallReason"), "첫 추천(PENDING)엔 방금 다시 찾았다는 문구가 붙으면 안 됨");
        assertNull(combo.get("studioReason"));
        assertNull(combo.get("dressReason"));
        assertNull(combo.get("makeupReason"));
    }

    @Test
    @Transactional
    public void testConfirmedSlotStillShowsConfirmedReason() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> confirmRequest = Map.of(
                "sessionId", sessionId, "category", "HALL", "action", "CONFIRM");
        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) slotBody.get("candidates")).get(0);

        assertEquals("확정하신 곳이에요", combo.get("hallReason"), "확정된 슬롯은 여전히 확정 문구가 있어야 함");
    }
}
