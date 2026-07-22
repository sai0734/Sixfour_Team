package com.wedding.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 사용자가 실제로 겪은 정확한 시나리오: 예산 3000만·강남·하객 200명·호텔형 홀·야외/자연 스튜디오
// 분위기·A라인 드레스·풀세트 메이크업으로 "AI에게 맡기기"를 하면, 이 조건들이 예산을 넘어서
// AI의 원래 조합 대신 규칙 기반 대체 조합(INDIVIDUAL_COMBO)이 그대로 나왔음 - 이 경로는 AI를
// 아예 거치지 않아서 모든 카테고리가 "예산 내 · 강남 지역에서 골랐어요" 같은 뻔한 문구뿐이었다.
// 고친 내용: 규칙 기반으로 대체된 조합이라도, 확정된 4곳을 그대로 AI한테 보여주고 왜 조건에 맞는지
// 설명만 새로 받아오도록 했다(AiPlanAiServiceImpl.explainWithAi). 실제 서버로 재현했던 것과 같은
// 파라미터로 확인한다. 실제 OpenAI 호출이 필요해서 느릴 수 있음. 롤백 기본값이라 테스트 끝나면
// DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanFallbackComboExplainedTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testFallbackComboStillGetsAiExplainedReasons() throws Exception {

        Map<String, Object> req = Map.ofEntries(
                Map.entry("budget", 30_000_000L),
                Map.entry("region", "강남"),
                Map.entry("groomName", "이재원"),
                Map.entry("brideName", "카리나"),
                Map.entry("weddingDate", "2026-11-26"),
                Map.entry("guestCount", 200),
                Map.entry("hallType", "HOTEL"),
                Map.entry("studioMood", "야외/자연"),
                Map.entry("dressStyle", "A라인"),
                Map.entry("makeupStyle", "FULL"),
                Map.entry("freeText", "넓은 홀로 부탁드려요"));

        String response = mockMvc.perform(post("/api/aiplan/ai")
                        .contentType("application/json; charset=UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) body.get("candidates")).get(0);

        log.info("sourceType={}, hallReason={}, studioReason={}, dressReason={}, makeupReason={}",
                combo.get("sourceType"), combo.get("hallReason"), combo.get("studioReason"),
                combo.get("dressReason"), combo.get("makeupReason"));

        List<String> genericPhrases = List.of("예산 내 · 강남 지역에서 골랐어요", "강남 지역에서 골랐어요",
                "조건에 맞게 골랐어요", "예산에 맞춰 조금 더 여유 있는 곳으로 조정했어요");

        for (String field : List.of("hallReason", "studioReason", "dressReason", "makeupReason")) {
            Object reason = combo.get(field);
            assertNotNull(reason, field + "는 값이 있어야 함");
            assertFalse(genericPhrases.contains(reason.toString()),
                    field + "가 규칙 기반 대체 조합이어도 뻔한 문구가 아니라 AI 설명이어야 함: " + reason);
        }
    }
}
