package com.wedding.aiplan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.dto.AiPlanQuickRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 실제 후보 조회/스코어링 로직은 AiPlanCandidateBuilder에 있음 (자세히 모드와 공유).
// 빠르게 모드는 카테고리 취향 없이(AiPlanCategoryPreferences.empty()) 호출하는 얇은 래퍼.
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class AiPlanQuickServiceImpl implements AiPlanQuickService {

    private final AiPlanCandidateBuilder candidateBuilder;

    @Override
    public AiPlanQuickResultDTO getQuickRecommendations(AiPlanQuickRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());

        AiPlanQuickResultDTO result = candidateBuilder.recommend(
                region, requestDTO.getBudget(), requestDTO.getGuestCount(), AiPlanCategoryPreferences.empty());

        // weddingDate는 세션이 없는 빠르게 모드에서도 결과 화면(ResultCards)에 그대로 보여주기 위해 실어보냄
        result.setWeddingDate(requestDTO.getWeddingDate());

        return result;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
