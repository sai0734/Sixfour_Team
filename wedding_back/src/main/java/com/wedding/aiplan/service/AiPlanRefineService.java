package com.wedding.aiplan.service;

import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.dto.AiPlanRefineRequestDTO;

public interface AiPlanRefineService {

    // 자유 발화 한 턴을 받아 카테고리별 확정/제외/재검토를 반영하고 재추천한 결과를 돌려줌
    AiPlanQuickResultDTO refine(AiPlanRefineRequestDTO requestDTO);

    // 바로 직전 턴 상태로 되돌림
    AiPlanQuickResultDTO rollback(Long sessionId);
}
