package com.wedding.aiplan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;

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
        AiPlanCategoryPreferences prefs = AiPlanCategoryPreferences.fromDetailRequest(requestDTO);

        return candidateBuilder.recommend(region, requestDTO.getBudget(), prefs);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
