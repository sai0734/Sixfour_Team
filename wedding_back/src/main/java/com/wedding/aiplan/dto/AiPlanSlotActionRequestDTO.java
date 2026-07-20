package com.wedding.aiplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 사이드패널 "확정"/"해제" 버튼 - AI 자유발화를 거치지 않고 슬롯 상태를 직접 바꿈
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanSlotActionRequestDTO {

    private Long sessionId;

    // "HALL" | "STUDIO" | "DRESS" | "MAKEUP"
    private String category;

    // "CONFIRM" | "UNLOCK"
    private String action;
}
