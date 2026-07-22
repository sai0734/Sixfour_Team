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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제로 서버를 띄워서 재현한 버그: AI 모드에서 홀/드레스/스튜디오/메이크업 전부 AI가 준
// 디테일한 이유가 잘 나오는데, 유독 홀만 "예산에 맞춰 조금 더 여유 있는 곳으로 조정했어요"라는
// 의미 없는 문구로 자꾸 바뀌어 있었음. 원인: AI가 고른 조합이 예산보다 많이 남으면
// fillComboTowardBudget이 남은 예산으로 더 비싼 홀로 갈아타는데(UPGRADE_ORDER 1순위가 HALL),
// 이때 업체 자체가 바뀌니 AI가 원래 준 설명은 안 맞아서 버려지고, 그 자리를 고정 문구로
// 채우고 있었다. AiPlanCandidateBuilder.pickReason으로 새 업체 기준 설명을 다시 만들도록 고쳤다.
// 실제 OpenAI 호출이 필요해서 느릴 수 있음. 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanHallUpgradeReasonTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testHallReasonIsNotGenericPlaceholderAfterBudgetUpgrade() throws Exception {

        // 조건 없이 예산만 넉넉하게 줘서 AI 조합보다 예산이 많이 남게 만든다 - fillComboTowardBudget이
        // 홀을 갈아탈 여지를 만들기 위함(실제로 이 시나리오에서 재현했음).
        Map<String, Object> req = Map.of("budget", 30_000_000L, "region", "강남");

        String response = mockMvc.perform(post("/api/aiplan/ai")
                        .contentType("application/json; charset=UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) body.get("candidates")).get(0);

        log.info("hallReason={}, studioReason={}, dressReason={}, makeupReason={}",
                combo.get("hallReason"), combo.get("studioReason"),
                combo.get("dressReason"), combo.get("makeupReason"));

        assertNotNull(combo.get("hallReason"), "홀도 이유 설명이 있어야 함");
        assertNotEquals("예산에 맞춰 조금 더 여유 있는 곳으로 조정했어요", combo.get("hallReason"),
                "홀이 예산 여유로 업체가 바뀌어도 의미 없는 고정 문구가 아니라 새 업체 기준 이유가 나와야 함");
    }
}
