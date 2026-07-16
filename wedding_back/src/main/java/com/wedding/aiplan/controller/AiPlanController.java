package com.wedding.aiplan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickRequestDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.service.AiPlanDetailService;
import com.wedding.aiplan.service.AiPlanQuickService;

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

}
