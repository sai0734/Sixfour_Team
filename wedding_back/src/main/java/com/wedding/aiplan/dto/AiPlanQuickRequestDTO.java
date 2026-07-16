package com.wedding.aiplan.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// "빠르게" 모드 필수 입력 4개 (문서 2번 표의 공통 필수 항목)
// groomName/brideName/weddingDate는 스코어링엔 안 쓰이지만, 이후 세션 저장/WeddingPlan 연동에 필요해서 같이 받음.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanQuickRequestDTO {

    private Long budget;

    private String region;

    private String groomName;

    private String brideName;

    private LocalDate weddingDate;
}
