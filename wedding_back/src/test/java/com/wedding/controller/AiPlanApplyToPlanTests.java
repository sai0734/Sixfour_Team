package com.wedding.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.global.util.JWTUtil;
import com.wedding.weddingplan.domain.WeddingPlan;
import com.wedding.weddingplan.repository.WeddingPlanRepository;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "이 결과 마이페이지에 담기" 버튼(POST /api/aiplan/session/{id}/apply-to-plan)이 웨딩플랜에 실제로
// 반영되는지, 남의 세션은 못 담는지 확인한다. 예산관리/체크리스트는 이제 이 버튼이 아니라
// 예약이 결제대기로 넘어올 때 반영되므로(ReservationServiceImpl.confirmByManager) 여기서는
// 안 다룸 - com.wedding.service.ReservationConfirmPrepSyncTests 참고.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanApplyToPlanTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WeddingPlanRepository weddingPlanRepository;

    private String tokenFor(String email, String nickname) {
        return JWTUtil.generateToken(Map.of(
                "email", email,
                "pw", "1111",
                "nickname", nickname,
                "social", false,
                "roleNames", List.of("USER")
        ), JWTUtil.ACCESS_TOKEN_MINUTES);
    }

    @Transactional
    @Test
    public void testApplyToPlanFillsWeddingPlanOnlyAndIsIdempotent() throws Exception {

        String token = tokenFor("aaa@aaa.com", "USER_aaa");
        LocalDate weddingDate = LocalDate.now().plusYears(2); // 클램프 안 걸리게 충분히 먼 미래

        // 강남 GARDEN 홀은 더미데이터상 "더라움"(cmno 11) 하나뿐이라 조합이 정확히 하나로 나옴 -> 세션 생성됨
        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .header("Authorization", "Bearer " + token)
                        .param("budget", "30000000")
                        .param("region", "강남")
                        .param("hallType", "GARDEN")
                        .param("weddingDate", weddingDate.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        // ── 1차 담기 ──
        String applyResponse = mockMvc.perform(post("/api/aiplan/session/" + sessionId + "/apply-to-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> applyBody = objectMapper.readValue(applyResponse, Map.class);
        assertEquals("SUCCESS", applyBody.get("RESULT"));

        WeddingPlan plan = weddingPlanRepository.findByMemberEmail("aaa@aaa.com").orElseThrow();
        assertEquals(weddingDate, plan.getWeddingDate());
        assertEquals(30_000_000L, plan.getTotalBudget());

        // ── 2차 담기 (같은 세션, 다시 눌러도 에러 없이 같은 웨딩플랜 1개만 유지돼야 함) ──
        mockMvc.perform(post("/api/aiplan/session/" + sessionId + "/apply-to-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        WeddingPlan planAfterSecondApply = weddingPlanRepository.findByMemberEmail("aaa@aaa.com").orElseThrow();
        assertEquals(plan.getWeddingPlanId(), planAfterSecondApply.getWeddingPlanId());
    }

    @Transactional
    @Test
    public void testApplyToPlanRejectsOtherMembersSession() throws Exception {

        String tokenAaa = tokenFor("aaa@aaa.com", "USER_aaa");
        String tokenBbb = tokenFor("bbb@bbb.com", "USER_bbb");

        String detailResponse = mockMvc.perform(get("/api/aiplan/detail")
                        .header("Authorization", "Bearer " + tokenAaa)
                        .param("region", "강남")
                        .param("hallType", "GARDEN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> detailBody = objectMapper.readValue(detailResponse, Map.class);
        Long sessionId = ((Number) detailBody.get("sessionId")).longValue();

        mockMvc.perform(post("/api/aiplan/session/" + sessionId + "/apply-to-plan")
                        .header("Authorization", "Bearer " + tokenBbb))
                .andExpect(status().isConflict());
    }

    @Transactional
    @Test
    public void testApplyToPlanRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/aiplan/session/1/apply-to-plan"))
                .andExpect(status().isUnauthorized());
    }
}
