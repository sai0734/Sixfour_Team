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

    // 조합이 정확히 1개로 나올 때만 채워짐(패키지 여러 개 나오는 빠르게 모드는 null) -
    // 6단계 리파인 대화(POST /api/aiplan/refine, /api/aiplan/rollback/{sessionId})에서 이 값을 씀.
    private Long sessionId;

    @Builder.Default
    private List<AiPlanPackageCandidateDTO> candidates = List.of();

    @Builder.Default
    private boolean regionRelaxed = false;

    private String message;
}
