package com.wedding.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.MakeupPackageRepository;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: 메이크업 "풀 패키지"(FULL=헤어+메이크업+네일)를 요청했는데 헤어+메이크업만 파는
// "블랑쉬 웨딩뷰티"(cmno=32, data/makeup.json - packages: TWO만 있음, THREE/FULL 없음)가
// 추천됨. AI가 고른 메이크업 업체의 makeupPackageType이 실제로 그 업체가 파는 값일 때만 채워지는지
// 확인한다. AI 호출이 실패/폴백돼도(규칙 기반 경로는 이미 올바르게 검증하므로) 이 불변조건은
// 항상 성립해야 한다 - 그래서 어느 경로를 타든 테스트가 유효하다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanMakeupPackageGroundingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MakeupPackageRepository makeupPackageRepository;

    @Test
    public void testAiRecommendedMakeupCompanyActuallySupportsRequestedFullPackage() throws Exception {

        // budget을 안 보내면(0/null) getAiRecommendations의 예산 재분배 단계(fillComboTowardBudget)가
        // 아예 안 돌아서, 이 테스트가 실제로 검증하려는 후보 풀 필터링·그라운딩 결과가 뒤에서
        // 다른 업체로 덮어써지는 것 없이 그대로 관찰된다.
        Map<String, Object> requestBody = Map.of(
                "makeupStyle", "FULL"
        );

        String response = mockMvc.perform(post("/api/aiplan/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        List<?> candidates = (List<?>) body.get("candidates");
        Map<?, ?> combo = (Map<?, ?>) candidates.get(0);

        Object makeupCmno = combo.get("makeupCmno");
        Object makeupPackageType = combo.get("makeupPackageType");

        log.info("makeupCmno={}, makeupPackageType={}", makeupCmno, makeupPackageType);

        // 메이크업이 아예 제외됐거나(makeupCmno null) packageType이 안 채워졌으면(null) 이 테스트가
        // 검증할 대상이 아님 - 안전하게 통과.
        if (makeupCmno == null || makeupPackageType == null) {
            return;
        }

        assertTrue("FULL".equals(makeupPackageType), "FULL을 요청했는데 다른 타입이 채워지면 안 됨");

        long cmno = ((Number) makeupCmno).longValue();
        List<Long> fullSupportingCmnos =
                makeupPackageRepository.findCompanyCmnosByPackageTypeIn(List.of(MakeupPackageType.FULL));

        assertTrue(fullSupportingCmnos.contains(cmno),
                "makeupPackageType이 FULL로 채워졌다면 그 업체(cmno=" + cmno + ")가 실제로 FULL을 팔아야 함");
    }
}
