package com.wedding.aiplan.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 다듬기(리파인) 대화 기록 한 턴 - "지금까지 뭐라고 말했었지" 다시 볼 때 씀
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanTurnDTO {

    private int turnNo;

    private String message;

    private LocalDateTime createdAt;
}
