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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제로 서버를 띄워서 재현한 버그: budget=2500만 + guestCount=300 + hallType=HOTEL +
// makeupStyle=FULL로 "AI에게 맡기기"를 하면 "500만원 늘리면 더 맞는 곳을 찾아드릴 수 있어요"가
// 뜨는데, 그 버튼대로 budget=3000만으로 다시 요청해도 완전히 똑같은 조합(가격 22,173,800원)과
// 똑같은 문구가 또 뜨면서 suggestedBudget만 3500만으로 올라갔다 - 이 카탈로그에서 살 수 있는
// 최선의 조합이 이미 22,173,800원이라 예산을 아무리 늘려도 더 비싼/나은 대안 자체가 없는데,
// 안내 문구는 "AI가 원래 원했던(취향 그대로인) 조합"과의 예산 차이만 보고 계산돼서 채울 수 없는
// 약속을 반복했다.
// 고친 내용: 이제 안내를 보여주기 전에 "실제로 그 예산으로 다시 찾아보면 더 비싼 조합이 나오는지"를
// 직접 검증한다(AiPlanAiServiceImpl/AiPlanCandidateBuilder) - 검증에서 안 나아지면 최초 요청부터
// 아예 안내를 안 보여준다. 그래서 이 시나리오에서는 첫 요청부터 suggestedBudget이 null이어야 한다.
// 실제 OpenAI 호출이 필요해서(문서/API 키가 있어야 통과) 느릴 수 있음. 롤백 기본값이라 테스트
// 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanBudgetBumpNoImprovementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testNoSuggestionWhenBumpingWontActuallyHelp() throws Exception {

        Map<?, ?> first = callAi(25_000_000L);
        Map<?, ?> firstCombo = (Map<?, ?>) firstCandidates(first).get(0);

        log.info("budget=2500만: sourceType={}, packagePrice={}, message={}, suggestedBudget={}",
                firstCombo.get("sourceType"), firstCombo.get("packagePrice"),
                first.get("message"), first.get("suggestedBudget"));

        // 이 카탈로그의 최선 조합은 예산을 늘려도 안 바뀌는 상한선이라, 첫 요청부터 "예산을
        // 늘리면 더 찾아준다"는 안내가 아예 뜨면 안 된다.
        assertNull(first.get("suggestedBudget"),
                "예산을 늘려도 더 나은 조합이 안 나오면 애초에 안내가 뜨면 안 됨");
        assertNull(first.get("message"));

        // 혹시 이 검증 로직 자체가 깨져서 안내가 다시 뜨는 회귀가 생기면, 최소한 그 안내는
        // 거짓이면 안 된다 - 제안한 예산으로 다시 요청했을 때 반드시 더 비싼(더 나은) 조합이
        // 나와야 한다는 걸 방어적으로 한 번 더 확인한다.
        Object suggestedBudget = first.get("suggestedBudget");
        if (suggestedBudget != null) {
            long bumped = ((Number) suggestedBudget).longValue();
            Map<?, ?> second = callAi(bumped);
            Map<?, ?> secondCombo = (Map<?, ?>) firstCandidates(second).get(0);

            long firstPrice = ((Number) firstCombo.get("packagePrice")).longValue();
            long secondPrice = ((Number) secondCombo.get("packagePrice")).longValue();

            assertTrue(secondPrice > firstPrice,
                    "안내가 떴다면 제안한 예산으로 다시 찾았을 때 반드시 더 비싼 조합이 나와야 함");
        }
    }

    private List<?> firstCandidates(Map<?, ?> body) {
        return (List<?>) body.get("candidates");
    }

    private Map<?, ?> callAi(long budget) throws Exception {
        Map<String, Object> req = Map.of(
                "budget", budget,
                "region", "강남",
                "guestCount", 300,
                "hallType", "HOTEL",
                "makeupStyle", "FULL");

        String response = mockMvc.perform(post("/api/aiplan/ai")
                        .contentType("application/json; charset=UTF-8")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, Map.class);
    }
}
