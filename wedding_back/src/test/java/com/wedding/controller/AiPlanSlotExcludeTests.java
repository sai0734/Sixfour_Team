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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 결과 화면 이미지 X 버튼 - "다른 곳에서 이미 예약해서 이 조합에서만 뺀다"는 뜻이라 대체 업체를
// 다시 찾아주면 안 되고(RECONSIDER), 그냥 EXCLUDED로 빼기만 해야 한다. 사이드패널 버튼 엔드포인트
// (POST /api/aiplan/slot)에 EXCLUDE 액션이 없어서 새로 추가함 - 정상 반영되는지 확인.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanSlotExcludeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testExcludeActionDropsCategoryWithoutReplacing() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> slotRequest = Map.of(
                "sessionId", sessionId,
                "category", "STUDIO",
                "action", "EXCLUDE"
        );

        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        List<?> candidates = (List<?>) slotBody.get("candidates");
        Map<?, ?> combo = (Map<?, ?>) candidates.get(0);

        log.info("after EXCLUDE: studioStatus={}, studioCmno={}",
                combo.get("studioStatus"), combo.get("studioCmno"));

        assertTrue("EXCLUDED".equals(combo.get("studioStatus")), "EXCLUDE 액션 후 상태가 EXCLUDED여야 함");
        assertNull(combo.get("studioCmno"), "EXCLUDE는 대체 업체를 찾지 않고 그냥 비워야 함");
    }

    // 확정된 카테고리에 X(EXCLUDE)를 눌러도 막지 않고 바로 제외돼야 함 - 예전엔 "먼저 해제해주세요"
    // 안내만 상단 배너로 뜨고 조용히 막혔는데, 카드 쪽으로 스크롤해 내려온 상태에선 그 안내가
    // 안 보여서 "눌러도 반응 없음"처럼 느껴진다는 피드백으로 확정 여부와 상관없이 항상 되게 바꿈.
    @Test
    @Transactional
    public void testExcludeWorksEvenWhenSlotConfirmed() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> confirmRequest = Map.of(
                "sessionId", sessionId, "category", "STUDIO", "action", "CONFIRM");
        mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        Map<String, Object> excludeRequest = Map.of(
                "sessionId", sessionId, "category", "STUDIO", "action", "EXCLUDE");
        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excludeRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) slotBody.get("candidates")).get(0);

        log.info("after CONFIRM then EXCLUDE: studioStatus={}", combo.get("studioStatus"));

        assertTrue("EXCLUDED".equals(combo.get("studioStatus")),
                "확정된 카테고리도 X를 누르면 바로 EXCLUDED가 돼야 함");
        assertNull(combo.get("studioCmno"));
    }
}
