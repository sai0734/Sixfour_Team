package com.wedding.aiplan.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.dto.AiPlanRefineRequestDTO;
import com.wedding.aiplan.dto.AiPlanSlotActionRequestDTO;
import com.wedding.aiplan.dto.AiPlanTurnDTO;
import com.wedding.aiplan.service.AiPlanAiService;
import com.wedding.aiplan.service.AiPlanDetailService;
import com.wedding.aiplan.service.AiPlanQuickService;
import com.wedding.aiplan.service.AiPlanRefineService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 비로그인 사용자도 결과를 볼 수 있어야 하므로(문서 8번) 이 컨트롤러엔 @PreAuthorize를 안 둠.
// 로그인 요구는 이후 단계(예약 연결/세션 저장)에서만 건다.
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/aiplan")
public class AiPlanController {

    private final AiPlanQuickService aiPlanQuickService;
    private final AiPlanDetailService aiPlanDetailService;
    private final AiPlanAiService aiPlanAiService;
    private final AiPlanRefineService aiPlanRefineService;

    // 예: GET /api/aiplan/quick?budget=30000000&region=강남&weddingDate=2027-05-01
    @GetMapping("/quick")
    public AiPlanQuickResultDTO quick(AiPlanQuickRequestDTO requestDTO) {

        log.info("AiPlan quick request: {}", requestDTO);

        return aiPlanQuickService.getQuickRecommendations(requestDTO);
    }

    // 예: GET /api/aiplan/detail?budget=30000000&region=강남&hallType=GARDEN&studioMood=내추럴
    @GetMapping("/detail")
    public AiPlanQuickResultDTO detail(AiPlanDetailRequestDTO requestDTO) {

        log.info("AiPlan detail request: {}", requestDTO);

        return aiPlanDetailService.getDetailRecommendations(requestDTO);
    }

    // 외부 AI API를 호출하는 비용이 드는 작업이라 GET 쿼리파라미터가 아니라 POST 바디로 받음
    @PostMapping("/ai")
    public AiPlanQuickResultDTO ai(@RequestBody AiPlanDetailRequestDTO requestDTO) {

        log.info("AiPlan AI request: {}", requestDTO);

        return aiPlanAiService.getAiRecommendations(requestDTO);
    }

    // 6단계 - 결과 화면에서 바로 자유 발화로 다듬기 ("스튜디오 빼줘" 등)
    @PostMapping("/refine")
    public AiPlanQuickResultDTO refine(@RequestBody AiPlanRefineRequestDTO requestDTO) {

        log.info("AiPlan refine request: {}", requestDTO);

        return aiPlanRefineService.refine(requestDTO);
    }

    // 상단 조합 히스토리 배지 - 예전 턴 하나를 클릭해서 그 시점 조합을 그대로 불러옴 (새 턴 안 만듦)
    @PostMapping("/session/{sessionId}/turn/{turnNo}")
    public AiPlanQuickResultDTO viewTurn(@PathVariable Long sessionId, @PathVariable int turnNo) {

        log.info("AiPlan view turn request: sessionId={}, turnNo={}", sessionId, turnNo);

        return aiPlanRefineService.viewTurn(sessionId, turnNo);
    }

    // 사이드패널 확정/해제 버튼 - AI 안 거치고 슬롯 상태 직접 반영
    @PostMapping("/slot")
    public AiPlanQuickResultDTO slot(@RequestBody AiPlanSlotActionRequestDTO requestDTO) {

        log.info("AiPlan slot action request: {}", requestDTO);

        return aiPlanRefineService.applySlotAction(requestDTO);
    }

    // 새로고침해도 결과가 안 날아가게 - 프론트가 URL에 들고 있는 sessionId로 복원
    @GetMapping("/session/{sessionId}")
    public AiPlanQuickResultDTO session(@PathVariable Long sessionId) {

        log.info("AiPlan session restore request: sessionId={}", sessionId);

        return aiPlanRefineService.getSession(sessionId);
    }

    // 다듬기 대화 기록 - 사이드바 "다듬은 기록 보기" 펼치기용
    @GetMapping("/session/{sessionId}/history")
    public List<AiPlanTurnDTO> refineHistory(@PathVariable Long sessionId) {

        log.info("AiPlan refine history request: sessionId={}", sessionId);

        return aiPlanRefineService.getRefineHistory(sessionId);
    }

    // "이 결과 마이페이지에 담기" - 이 경로는 비로그인 허용 목록에 없어서 로그인이 필수다
    @PostMapping("/session/{sessionId}/apply-to-plan")
    public Map<String, String> applyToPlan(@PathVariable Long sessionId) {

        log.info("AiPlan apply to plan request: sessionId={}", sessionId);

        return aiPlanRefineService.applyToPlan(sessionId);
    }

}
