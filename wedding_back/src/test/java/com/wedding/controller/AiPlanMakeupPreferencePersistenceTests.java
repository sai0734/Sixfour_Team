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
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.MakeupPackageRepository;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트 2단계: 예산 초과로 조건을 다 내려놓고 재검색하는 경로에서 메이크업 취향까지 같이
// 버려지던 문제(AiPlanCandidateBuilder.recommend / AiPlanAiServiceImpl budgetFit), 그리고
// "다시 찾기"/다듬기에서 세션에 원래 취향이 없어서 매번 무시되던 문제(AiPlanRefineServiceImpl)를
// 같이 확인한다. 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanMakeupPreferencePersistenceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MakeupPackageRepository makeupPackageRepository;

    private List<Long> fullSupportingCmnos() {
        return makeupPackageRepository.findCompanyCmnosByPackageTypeIn(List.of(MakeupPackageType.FULL));
    }

    // 홀타입까지 같이 요청해서 조합 가격이 예산을 훌쩍 넘게 만든 뒤(실제 사용자가 겪은 상황과
    // 동일), 그래도 메이크업 FULL 취향만은 지켜지는지 확인한다.
    @Transactional
    @Test
    public void testBudgetOverflowFallbackKeepsMakeupPreference() throws Exception {

        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "40000000")
                        .param("region", "강남")
                        .param("hallType", "GARDEN")
                        .param("makeupStyle", "FULL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        List<?> candidates = (List<?>) body.get("candidates");
        Map<?, ?> combo = (Map<?, ?>) candidates.get(0);

        Object makeupCmno = combo.get("makeupCmno");
        Object makeupPackageType = combo.get("makeupPackageType");
        log.info("budget-fallback: sourceType={}, makeupCmno={}, makeupPackageType={}",
                combo.get("sourceType"), makeupCmno, makeupPackageType);

        // 예산 부족으로 폴백 경로를 탔더라도(패키지가 아니라 개별조합으로), 메이크업만은 요청한
        // FULL을 파는 업체가 나와야 한다.
        assertTrue(makeupCmno != null, "메이크업 후보 자체가 아예 없으면 안 됨");
        assertTrue("FULL".equals(makeupPackageType), "예산 폴백에서도 메이크업 취향(FULL)은 유지돼야 함");
        assertTrue(fullSupportingCmnos().contains(((Number) makeupCmno).longValue()),
                "makeupPackageType=FULL이면 그 업체가 실제로 FULL을 팔아야 함");
    }

    // 처음 요청(FULL)으로 세션을 만든 뒤, 사이드패널 "다시 찾기"(RECONSIDER)로 메이크업만 다시
    // 찾아도 여전히 FULL을 파는 업체가 나오는지 확인한다 - 세션에 취향이 저장 안 돼 있으면 이
    // 시점에 아무 업체나 나와서 실패한다.
    @Transactional
    @Test
    public void testReconsiderKeepsMakeupPreferenceFromSession() throws Exception {

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남")
                        .param("makeupStyle", "FULL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        List<?> detailCandidates = (List<?>) detailBody.get("candidates");
        Map<?, ?> initialCombo = (Map<?, ?>) detailCandidates.get(0);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        assertTrue("FULL".equals(initialCombo.get("makeupPackageType")), "초기 추천부터 FULL이 맞아야 함");

        Map<String, Object> slotRequest = Map.of(
                "sessionId", sessionId,
                "category", "MAKEUP",
                "action", "RECONSIDER"
        );

        String slotResponse = mockMvc.perform(post("/api/aiplan/slot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> slotBody = objectMapper.readValue(slotResponse, Map.class);
        List<?> slotCandidates = (List<?>) slotBody.get("candidates");
        Map<?, ?> reconsideredCombo = (Map<?, ?>) slotCandidates.get(0);

        Object makeupCmno = reconsideredCombo.get("makeupCmno");
        Object makeupPackageType = reconsideredCombo.get("makeupPackageType");
        log.info("after RECONSIDER: makeupCmno={}, makeupPackageType={}", makeupCmno, makeupPackageType);

        assertTrue(makeupCmno != null, "다시 찾기 후에도 메이크업 후보가 있어야 함");
        assertTrue("FULL".equals(makeupPackageType), "다시 찾기 후에도 세션에 저장된 FULL 취향이 유지돼야 함");
        assertTrue(fullSupportingCmnos().contains(((Number) makeupCmno).longValue()),
                "makeupPackageType=FULL이면 그 업체가 실제로 FULL을 팔아야 함");
    }
}
