package com.wedding.aiplan.service;

import java.util.List;
import java.util.Map;

import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.dto.AiPlanRefineRequestDTO;
import com.wedding.aiplan.dto.AiPlanSlotActionRequestDTO;
import com.wedding.aiplan.dto.AiPlanTurnDTO;

public interface AiPlanRefineService {

    AiPlanQuickResultDTO refine(AiPlanRefineRequestDTO requestDTO);

    // 상단 조합 히스토리 배지 - 임의의 예전 턴을 클릭해서 그 시점 조합을 그대로 불러온다.
    // "본다"는 개념이라 새 턴을 기록에 추가하지 않는다(둘러보기만으로 배지가 계속 늘어나면
    // 안 됨) - 그 상태에서 실제로 뭔가 더 바꾸면 그때 비로소 새 턴이 생긴다.
    AiPlanQuickResultDTO viewTurn(Long sessionId, int turnNo);

    // 사이드패널 확정/해제/다시찾기 버튼 - AI를 거치지 않고 슬롯 상태를 직접 반영
    AiPlanQuickResultDTO applySlotAction(AiPlanSlotActionRequestDTO requestDTO);

    // 새로고침 복원 - 프론트가 URL에 들고 있는 sessionId로 현재 세션 상태를 다시 조회
    AiPlanQuickResultDTO getSession(Long sessionId);

    // 다듬기 대화 기록 - 사이드바 "다듬은 기록 보기" 펼치기용
    List<AiPlanTurnDTO> getRefineHistory(Long sessionId);

    // "이 결과 마이페이지에 담기" - 확정 조합을 웨딩플랜/예산관리/체크리스트에 반영 (로그인 필수)
    Map<String, String> applyToPlan(Long sessionId);
}
