package com.wedding.aiplan.service;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;

public interface AiPlanDetailService {

    // AI 호출 없이, 카테고리별 취향(홀 분위기/스튜디오 분위기/드레스 스타일/메이크업 스타일)까지 반영해서 추천
    AiPlanQuickResultDTO getDetailRecommendations(AiPlanDetailRequestDTO requestDTO);

}
