package com.wedding.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.service.AiPlanSessionSupport;
import com.wedding.member.dto.MemberDTO;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// JWTCheckFilter가 aiplan 공개 경로에서도 로그인 토큰이 있으면 SecurityContext를 채워두게
// 바꿨는데, 그 값을 AiPlanSessionSupport.createSession()이 실제로 세션에 남기는지 확인.
@SpringBootTest
@Log4j2
public class AiPlanSessionSupportTests {

    @Autowired
    private AiPlanSessionSupport aiPlanSessionSupport;

    @Transactional
    @Test
    public void testCreateSessionFillsMemberEmailWhenLoggedIn() {

        MemberDTO memberDTO = new MemberDTO("aaa@aaa.com", "1111", "USER_aaa", false, java.util.List.of("USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(memberDTO, "1111", memberDTO.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            AiPlanPackageCandidateDTO combo = AiPlanPackageCandidateDTO.builder().build();

            AiPlanSession session = aiPlanSessionSupport.createSession(
                    30_000_000L, "강남", null, null, null, null, "DETAIL", combo);

            log.info(session);

            assertEquals("aaa@aaa.com", session.getMemberEmail());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Transactional
    @Test
    public void testCreateSessionLeavesMemberEmailNullWhenAnonymous() {

        SecurityContextHolder.clearContext();

        AiPlanPackageCandidateDTO combo = AiPlanPackageCandidateDTO.builder().build();

        AiPlanSession session = aiPlanSessionSupport.createSession(
                30_000_000L, "강남", null, null, null, null, "DETAIL", combo);

        log.info(session);

        assertNull(session.getMemberEmail());
    }
}
