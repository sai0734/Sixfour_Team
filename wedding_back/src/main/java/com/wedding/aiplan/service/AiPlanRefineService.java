package com.wedding.aiplan.service;

import java.util.List;

import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.dto.AiPlanRefineRequestDTO;
import com.wedding.aiplan.dto.AiPlanSlotActionRequestDTO;
import com.wedding.aiplan.dto.AiPlanTurnDTO;

public interface AiPlanRefineService {

    AiPlanQuickResultDTO refine(AiPlanRefineRequestDTO requestDTO);

    AiPlanQuickResultDTO rollback(Long sessionId);

    // 사이드패널 확정/해제/다시찾기 버튼 - AI를 거치지 않고 슬롯 상태를 직접 반영
    AiPlanQuickResultDTO applySlotAction(AiPlanSlotActionRequestDTO requestDTO);

    // 새로고침 복원 - 프론트가 URL에 들고 있는 sessionId로 현재 세션 상태를 다시 조회
    AiPlanQuickResultDTO getSession(Long sessionId);

    // 다듬기 대화 기록 - 사이드바 "다듬은 기록 보기" 펼치기용
    List<AiPlanTurnDTO> getRefineHistory(Long sessionId);
}
