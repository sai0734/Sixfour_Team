package com.wedding.aiplan.service;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;

public interface AiPlanAiService {

    // SQL로 좁힌 후보 목록 안에서만 AI가 고르게 하고, 서버가 다시 검증함.
    // AI 호출/파싱/그라운딩 검증 중 하나라도 실패하면 4단계(자세히 모드) 로직으로 자동 폴백함.
    AiPlanQuickResultDTO getAiRecommendations(AiPlanDetailRequestDTO requestDTO);

}
