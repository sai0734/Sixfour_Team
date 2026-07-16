package com.wedding.aiplan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.company.domain.HallType;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class AiPlanDetailServiceImpl implements AiPlanDetailService {

    private final AiPlanCandidateBuilder candidateBuilder;

    @Override
    public AiPlanQuickResultDTO getDetailRecommendations(AiPlanDetailRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());

        AiPlanCategoryPreferences prefs = AiPlanCategoryPreferences.of(
                parseHallType(requestDTO.getHallType()),
                blankToNull(requestDTO.getStudioMood()),
                blankToNull(requestDTO.getDressStyle()),
                blankToNull(requestDTO.getMakeupStyle()));

        return candidateBuilder.recommend(region, requestDTO.getBudget(), prefs);
    }

    // 프론트에서 잘못된 값을 보내도 500 대신 "취향 없음"으로 안전하게 무시
    private HallType parseHallType(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return HallType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown hallType value ignored: {}", raw);
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
