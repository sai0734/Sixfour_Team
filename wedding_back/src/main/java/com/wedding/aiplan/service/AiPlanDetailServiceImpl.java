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
@Transactional
public class AiPlanDetailServiceImpl implements AiPlanDetailService {

    private final AiPlanCandidateBuilder candidateBuilder;
    private final AiPlanSessionSupport sessionSupport;

    @Override
    public AiPlanQuickResultDTO getDetailRecommendations(AiPlanDetailRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());
        AiPlanCategoryPreferences prefs = AiPlanCategoryPreferences.fromDetailRequest(requestDTO);

        AiPlanQuickResultDTO result = candidateBuilder.recommend(
                region, requestDTO.getBudget(), requestDTO.getGuestCount(), prefs);

        // 조합이 정확히 하나로 나왔을 때만 세션을 만든다 - 패키지가 여러 개 나오면 아직 확정한 게
        // 없어서 리파인 대화(6단계)를 붙일 대상이 없음
        if (result.getCandidates().size() == 1) {
            var session = sessionSupport.createSession(
                    requestDTO.getBudget(), region, requestDTO.getWeddingDate(), "DETAIL", result.getCandidates().get(0));
            result.setSessionId(session.getSessionId());
        }

        // weddingDate는 세션 유무와 무관하게 항상 실어보냄 - 결과 화면에 "결혼 예정일" 표시용
        result.setWeddingDate(requestDTO.getWeddingDate());

        return result;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
