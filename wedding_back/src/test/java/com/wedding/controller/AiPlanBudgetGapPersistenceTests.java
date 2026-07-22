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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: "예산을 더 쓰면 더 맞는 곳을 찾아준다"는 안내 문구가 최초 추천 때만 뜨고, 다른
// 페이지 갔다가 돌아오면(=새로고침/세션 복원, GET /api/aiplan/session/{id}) 사라져 있었음.
// 원인은 그 안내가 세션에 저장되는 값이 아니라 최초 추천 응답에만 실려있던 일회성 필드였기
// 때문 - 세션 복원 시에도 지금 조합 합계가 여전히 예산을 넘으면 매번 다시 계산해서 붙이도록
// 고쳤다(AiPlanCandidateBuilder.budgetSuggestion, AiPlanRefineServiceImpl.getSession에서 재사용).
// 추가로 제안 증액분은 최대 500만원까지만 보여주도록 상한도 걸었다. 롤백 기본값이라 테스트
// 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanBudgetGapPersistenceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testBudgetGapMessageReappearsAfterSessionRestore() throws Exception {

        // 아주 낮은 예산으로 요청 - 더미데이터 최저가로만 골라도 이 예산은 못 맞춘다(홀 최저가만
        // 해도 수백만원대). 최초 추천 응답 자체는 조건에 따라 메시지만 붙고 제안 예산은 안 붙는
        // 경로를 탈 수도 있어서(하객수/취향 조건이 없을 때), 여기서는 그 값을 전제로 삼지 않고
        // 세션 복원(getSession) 쪽만 검증한다 - 그게 이번에 고친 부분이다.
        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "1000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        log.info("initial: message={}, suggestedBudget={}",
                detailBody.get("message"), detailBody.get("suggestedBudget"));

        String sessionResponse = mockMvc.perform(get("/api/aiplan/session/" + sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> sessionBody = objectMapper.readValue(sessionResponse, Map.class);

        log.info("after session restore: message={}, suggestedBudget={}",
                sessionBody.get("message"), sessionBody.get("suggestedBudget"));

        assertNotNull(sessionBody.get("message"), "세션 복원 후에도 예산 관련 안내가 남아있어야 함");
        assertTrue(sessionBody.get("message").toString().contains("예산"),
                "복원된 메시지도 예산 안내 내용이어야 함");
        assertNotNull(sessionBody.get("suggestedBudget"), "세션 복원 후에도 제안 예산이 다시 계산돼야 함");

        // 실제 부족분은 훨씬 클 텐데(예산 100만원으로 조합을 맞출 순 없음), 제안 증액분은
        // 최대 500만원까지만이어야 한다 - "1200만원 더 넣어야 한다" 같은 비현실적인 숫자를
        // 그대로 보여주지 않기 위한 상한선.
        long increment = ((Number) sessionBody.get("suggestedBudget")).longValue() - 1_000_000L;
        assertTrue(increment <= 5_000_000L,
                "제안 증액분은 500만원을 넘으면 안 됨 (실제 증액=" + increment + ")");
    }
}
