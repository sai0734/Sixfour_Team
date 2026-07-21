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
import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.repository.AiPlanSessionRepository;
import com.wedding.global.util.JWTUtil;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// JWTCheckFilter가 aiplan 공개 경로에서 로그인 토큰을 있으면 파싱해서 SecurityContext를 채우고,
// AiPlanSessionSupport.createSession()이 그 이메일을 세션에 남기는지 실제 HTTP 요청으로 확인.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanJwtOptionalAuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiPlanSessionRepository aiPlanSessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    @Test
    public void testDetailWithLoginTokenFillsMemberEmail() throws Exception {

        String token = JWTUtil.generateToken(Map.of(
                "email", "aaa@aaa.com",
                "pw", "1111",
                "nickname", "USER_aaa",
                "social", false,
                "roleNames", List.of("USER")
        ), JWTUtil.ACCESS_TOKEN_MINUTES);

        String responseJson = mockMvc.perform(get("/api/aiplan/detail")
                        .header("Authorization", "Bearer " + token)
                        .param("region", "강남")
                        .param("hallType", "GARDEN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(responseJson, Map.class);
        Object sessionIdObj = body.get("sessionId");
        assertNotNull(sessionIdObj, "GARDEN 홀 단일 후보라 세션이 생겨야 함");

        Long sessionId = ((Number) sessionIdObj).longValue();
        AiPlanSession session = aiPlanSessionRepository.findById(sessionId).orElseThrow();

        assertEquals("aaa@aaa.com", session.getMemberEmail());
    }

    @Transactional
    @Test
    public void testDetailWithoutTokenLeavesMemberEmailNull() throws Exception {

        String responseJson = mockMvc.perform(get("/api/aiplan/detail")
                        .param("region", "강남")
                        .param("hallType", "GARDEN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(responseJson, Map.class);
        Object sessionIdObj = body.get("sessionId");
        assertNotNull(sessionIdObj);

        Long sessionId = ((Number) sessionIdObj).longValue();
        AiPlanSession session = aiPlanSessionRepository.findById(sessionId).orElseThrow();

        assertNull(session.getMemberEmail());
    }
}
