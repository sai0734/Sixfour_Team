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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 사용자 요청: (1) "플랜 수정하기"로 새 업체를 다시 받으면 왜 그 업체를 골랐는지 AI가 다시
// 설명해줘야 함 (2) 홀을 확정해두고 "나머지 새로운 걸로 다시 추천해줘"처럼 확정된 카테고리까지
// 건드리려는 발화를 하면, 조용히 무시하지 말고 "이미 확정되어 있어서 수정하지 못했다"는 안내가
// 떠야 함. 실제 OpenAI 호출이 필요해서 느릴 수 있음. 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanRefineBlockedAndReasonTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testRefineReconsiderGetsAiExplainedReason() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> refineRequest = Map.of(
                "sessionId", sessionId,
                "message", "스튜디오 다른 걸로 바꿔줘, 더 화사한 느낌으로");

        String refineResponse = mockMvc.perform(post("/api/aiplan/refine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refineRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> refineBody = objectMapper.readValue(refineResponse, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) refineBody.get("candidates")).get(0);
        Object studioReason = combo.get("studioReason");

        log.info("studioReason after refine RECONSIDER={}", studioReason);

        List<String> genericPhrases = List.of("예산 내 · 강남 지역에서 골랐어요", "강남 지역에서 골랐어요",
                "조건에 맞게 골랐어요", "예산에 맞춰 조금 더 여유 있는 곳으로 조정했어요");

        assertNotNull(studioReason, "다듬기로 다시 고른 스튜디오도 이유가 있어야 함");
        assertTrue(!genericPhrases.contains(studioReason.toString()),
                "다듬기로 다시 고른 스튜디오는 뻔한 계산 문구가 아니라 AI 설명이어야 함: " + studioReason);
    }

    @Test
    @Transactional
    public void testRefineMentioningConfirmedCategoryShowsBlockedMessage() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        Map<String, Object> confirmHall = Map.of(
                "sessionId", sessionId, "category", "HALL", "action", "CONFIRM");
        mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmHall)))
                .andExpect(status().isOk());

        Map<String, Object> refineRequest = Map.of(
                "sessionId", sessionId,
                "message", "홀 빼고 나머지 전부 새로운 걸로 다시 추천해줘");

        String refineResponse = mockMvc.perform(post("/api/aiplan/refine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refineRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> refineBody = objectMapper.readValue(refineResponse, Map.class);
        Object message = refineBody.get("message");

        log.info("message after refine touching confirmed HALL={}", message);

        assertNotNull(message, "확정된 카테고리를 같이 바꾸려고 하면 안내 문구가 떠야 함");
        assertTrue(message.toString().contains("확정") && message.toString().contains("웨딩홀"),
                "확정된 카테고리(웨딩홀)를 못 바꿨다는 안내여야 함: " + message);
    }
}
