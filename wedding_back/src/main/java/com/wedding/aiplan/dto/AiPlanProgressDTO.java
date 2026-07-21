package com.wedding.aiplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 메인 화면 "AI 매칭 진행중" 위젯용 - 로그인 회원의 가장 최근 AI 웨딩플랜 세션에서
// 홀/드레스/스튜디오 슬롯이 각각 얼마나 진행됐는지 퍼센트로 내려준다.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanProgressDTO {

    private boolean hasSession;

    private int hallPercent;

    private int dressPercent;

    private int studioPercent;
}
