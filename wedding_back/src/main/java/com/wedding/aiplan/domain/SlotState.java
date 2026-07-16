package com.wedding.aiplan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// 슬롯(홀/스튜디오/드레스/메이크업) 하나의 상태 값 객체.
// AiPlanSession에서 4번(hallSlot/studioSlot/dressSlot/makeupSlot) 재사용되며,
// 카테고리 자체는 어느 필드에 담기느냐로 구분되고 이 클래스 자체엔 카테고리 값을 안 둠.
@Embeddable
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlotState {

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SlotStatus status = SlotStatus.PENDING;

    // 확정/재검토 대상으로 선택된 업체의 cmno (Company.cmno 참조, 아직 하나도 안 골랐으면 null)
    private Long selectedCmno;

    // "스튜디오는 내추럴한 느낌으로" 같이 이 슬롯에 대해 사용자가 남긴 자유 메모
    @Column(length = 500)
    private String note;

    public void changeStatus(SlotStatus status) {
        this.status = status;
    }

    public void changeSelectedCmno(Long selectedCmno) {
        this.selectedCmno = selectedCmno;
    }

    public void changeNote(String note) {
        this.note = note;
    }

    public static SlotState empty() {
        return SlotState.builder().status(SlotStatus.PENDING).build();
    }
}
