package com.wedding.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.service.AiPlanDetailService;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Log4j2
public class AiPlanDetailServiceTests {

    @Autowired
    private AiPlanDetailService aiPlanDetailService;

    // 취향을 하나라도 넣으면 패키지 말고 개별 조합으로 가야 함 + 더미데이터상 강남 GARDEN 홀은
    // "더라움"(cmno 11) 하나뿐이라 정확히 그게 뽑히는지까지 확인
    @Test
    public void testDetailWithHallTypeMatchesExactHall() {

        AiPlanDetailRequestDTO requestDTO = AiPlanDetailRequestDTO.builder()
                .region("강남")
                .hallType("GARDEN")
                .build();

        AiPlanQuickResultDTO result = aiPlanDetailService.getDetailRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty());
        AiPlanPackageCandidateDTO combo = result.getCandidates().get(0);
        assertEquals("INDIVIDUAL_COMBO", combo.getSourceType());
        assertEquals(11L, combo.getHallCmno());
    }

    // 드레스 스타일 취향(styleTags 매칭)이 반영되는지
    @Test
    public void testDetailWithDressStyle() {

        AiPlanDetailRequestDTO requestDTO = AiPlanDetailRequestDTO.builder()
                .dressStyle("미니")
                .build();

        AiPlanQuickResultDTO result = aiPlanDetailService.getDetailRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty());
        assertEquals("INDIVIDUAL_COMBO", result.getCandidates().get(0).getSourceType());
    }

    // 메이크업은 구조화된 태그가 없어서 description 키워드 매칭으로 대체되는 경로 확인
    @Test
    public void testDetailWithMakeupStyle() {

        AiPlanDetailRequestDTO requestDTO = AiPlanDetailRequestDTO.builder()
                .makeupStyle("내추럴")
                .build();

        AiPlanQuickResultDTO result = aiPlanDetailService.getDetailRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty());
        AiPlanPackageCandidateDTO combo = result.getCandidates().get(0);
        log.info(" - makeup: " + combo.getMakeupName());
    }

    // 없는 취향 값(오타 등)을 보내도 500 없이 그냥 취향 없는 것처럼 동작해야 함
    @Test
    public void testDetailWithInvalidHallTypeIgnoredSafely() {

        AiPlanDetailRequestDTO requestDTO = AiPlanDetailRequestDTO.builder()
                .region("강남")
                .hallType("NOT_A_REAL_TYPE")
                .build();

        AiPlanQuickResultDTO result = aiPlanDetailService.getDetailRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty());
    }
}
