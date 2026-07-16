package com.wedding.aiplan.service;

import com.wedding.aiplan.dto.AiPlanQuickRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;

public interface AiPlanQuickService {

    // AI 호출 없이 SQL 후보 조회 + 예산/거리 기반 스코어링만으로 상위 5개 패키지 추천
    AiPlanQuickResultDTO getQuickRecommendations(AiPlanQuickRequestDTO requestDTO);

}
