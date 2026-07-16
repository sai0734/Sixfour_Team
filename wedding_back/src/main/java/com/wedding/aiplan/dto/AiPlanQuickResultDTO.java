package com.wedding.aiplan.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 문서 5번: 후보 0건이면 "없음" 대신 조건 완화 역제안. regionRelaxed=true면
// 프론트에서 "지역 조건 없이 추천했어요" 같은 안내를 message로 보여주면 됨.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanQuickResultDTO {

    @Builder.Default
    private List<AiPlanPackageCandidateDTO> candidates = List.of();

    @Builder.Default
    private boolean regionRelaxed = false;

    private String message;
}
