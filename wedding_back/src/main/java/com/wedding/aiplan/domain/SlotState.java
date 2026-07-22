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

    // "왜 이 업체를 골랐는지" - AI가 그 순간 만들어준 설명(또는 규칙 기반 다시찾기의 설명)을
    // 여기 저장해둔다. 세션에 안 남기고 매번 새로 계산하면(예산/지역 매칭 정도) 정보가 너무
    // 빈약해져서, 처음 고를 때 만든 설명을 그대로 들고 다니다가 확정/새로고침/조합 히스토리
    // 배지 등 어떤 경로로 다시 봐도 똑같이 보여준다. 업체가 바뀌면(다시찾기) 그때 새로 채워짐.
    @Column(length = 500)
    private String pickReason;

    public void changeStatus(SlotStatus status) {
        this.status = status;
    }

    public void changeSelectedCmno(Long selectedCmno) {
        this.selectedCmno = selectedCmno;
    }

    public void changeNote(String note) {
        this.note = note;
    }

    public void changePickReason(String pickReason) {
        this.pickReason = pickReason;
    }

    public static SlotState empty() {
        return SlotState.builder().status(SlotStatus.PENDING).build();
    }
}
