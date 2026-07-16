package com.wedding.aiplan.domain;

// 슬롯(홀/스튜디오/드레스/메이크업) 하나의 진행 상태
// PENDING: 아직 AI가 후보만 제시한 상태 (재검토 대상)
// CONFIRMED: 사용자가 "이걸로 할게" 확정
// EXCLUDED: 사용자가 "이건 빼줘" 제외 (예산 재분배 대상)
public enum SlotStatus {
    PENDING,
    CONFIRMED,
    EXCLUDED
}
