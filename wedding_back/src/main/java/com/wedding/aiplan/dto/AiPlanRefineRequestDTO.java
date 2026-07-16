package com.wedding.aiplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 6단계 리파인 대화 - "스튜디오 빼줘" 같은 자유 발화 한 턴
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanRefineRequestDTO {

    private Long sessionId;

    private String message;
}
