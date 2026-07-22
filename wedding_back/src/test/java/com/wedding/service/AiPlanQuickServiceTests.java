package com.wedding.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanQuickRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.service.AiPlanQuickService;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Log4j2
public class AiPlanQuickServiceTests {

    @Autowired
    private AiPlanQuickService aiPlanQuickService;

    // 강남+예산 2천만원이면 기존 패키지 중에 적합한 게 있어서(패키지 시딩값이 대체로 그 근처)
    // PACKAGE 후보가 나와야 함
    @Test
    public void testQuickWithFittingPackage() {

        AiPlanQuickRequestDTO requestDTO = AiPlanQuickRequestDTO.builder()
                .budget(20_000_000L)
                .region("강남")
                .groomName("철수")
                .brideName("영희")
                .weddingDate(LocalDate.of(2027, 5, 1))
                .build();

        AiPlanQuickResultDTO result = aiPlanQuickService.getQuickRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty(), "강남+2천만원엔 적합한 패키지가 있어야 함");
        assertEquals("PACKAGE", result.getCandidates().get(0).getSourceType());

        for (AiPlanPackageCandidateDTO c : result.getCandidates()) {
            log.info(" - " + c.getName() + " | " + c.getReason() + " | " + c.getSourceType());
        }
    }

    // 홀 업체가 전부 강남구뿐이라 인천 지역엔 적합한 패키지가 있을 수 없음 -> 개별 조합으로 대체돼야 함
    @Test
    public void testQuickWithNoFittingPackageFallsBackToIndividualCombo() {

        AiPlanQuickRequestDTO requestDTO = AiPlanQuickRequestDTO.builder()
                .budget(20_000_000L)
                .region("인천")
                .build();

        AiPlanQuickResultDTO result = aiPlanQuickService.getQuickRecommendations(requestDTO);

        log.info(result);

        assertFalse(result.getCandidates().isEmpty(), "패키지가 없어도 개별 조합으로는 나와야 함");
        assertEquals("INDIVIDUAL_COMBO", result.getCandidates().get(0).getSourceType());
        // "일부는 지역 조건 밖에서 골랐어요" 같은 설명 문구는 결과 화면 배너에서 없앴다(예산
        // 늘리기 안내 전용으로 정리) - 지역이 풀렸는지는 이 regionRelaxed 플래그로 확인한다.
        assertTrue(result.isRegionRelaxed(), "인천엔 업체 자체가 없어서 지역 조건이 풀렸어야 함");

        AiPlanPackageCandidateDTO combo = result.getCandidates().get(0);
        log.info(" - 조합: " + combo.getHallName() + " / " + combo.getDressName()
                + " / " + combo.getStudioName() + " / " + combo.getMakeupName() + " | " + combo.getReason());
    }

    // 예산/지역 둘 다 없는 극단 케이스도 죽지 않고 뭔가는 추천해야 함 (거리순 폴백)
    @Test
    public void testQuickWithNoFilters() {

        AiPlanQuickRequestDTO requestDTO = AiPlanQuickRequestDTO.builder().build();

        AiPlanQuickResultDTO result = aiPlanQuickService.getQuickRecommendations(requestDTO);

        log.info(result);

        List<AiPlanPackageCandidateDTO> candidates = result.getCandidates();
        assertFalse(candidates.isEmpty(), "조건이 없어도 전체 중에서 추천은 되어야 함");
    }
}
