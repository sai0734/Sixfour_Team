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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: 결과 화면에서 "스튜디오 다른 걸로 바꿔줘"(다시 찾기/다듬기)를 눌러도 실제로는
// 그대로였음. pickBestCompanyPlain/findStyled* 계열이 지역/예산/취향이 그대로면 항상 같은 업체를
// 결정적으로 돌려주기 때문 - "다시 찾기"가 지금 고른 업체를 후보에서 빼지 않고 똑같이 재검색했다.
// 사이드패널 "다시 찾기" 버튼(POST /api/aiplan/slot, action=RECONSIDER)으로 실제 업체가 바뀌는지
// 확인한다. 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanReconsiderChangesCompanyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    public void testReconsiderStudioReturnsDifferentCompany() throws Exception {
        assertReconsiderChangesCompany("STUDIO", "studioCmno");
    }

    @Test
    @Transactional
    public void testReconsiderDressReturnsDifferentCompany() throws Exception {
        assertReconsiderChangesCompany("DRESS", "dressCmno");
    }

    private void assertReconsiderChangesCompany(String category, String cmnoField) throws Exception {

        // 취향 없이 지역/예산만으로 - pickBestCompanyPlain(결정적 최선 1곳) 경로를 그대로 탄다.
        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        List<?> candidates = (List<?>) detailBody.get("candidates");
        Map<?, ?> initialCombo = (Map<?, ?>) candidates.get(0);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();
        Number initialCmno = (Number) initialCombo.get(cmnoField);

        Map<String, Object> slotRequest = Map.of(
                "sessionId", sessionId,
                "category", category,
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
        Number reconsideredCmno = (Number) reconsideredCombo.get(cmnoField);

        log.info("{}: before={}, after RECONSIDER={}", category, initialCmno, reconsideredCmno);

        assertNotEquals(initialCmno.longValue(), reconsideredCmno.longValue(),
                category + " 다시 찾기를 눌렀는데 같은 업체가 그대로 나오면 안 됨");
    }
}
