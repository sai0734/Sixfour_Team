package com.wedding.aiplan.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// "이번 추천 대화에서의 임시 상태". WeddingPlan(확정된 기본정보)과는 별개로,
// 여러 번 다시 추천받아도 서로 꼬이지 않도록 세션 단위로 분리해서 관리함.
// 비로그인 사용자도 결과를 볼 수 있어야 하므로 memberEmail은 null 허용
// (문서 8번: 로그인은 "이 조합으로 예약 진행"이나 세션 저장 시점에만 요구).
@Entity
@Table(name = "tbl_ai_plan_session")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    // 비로그인 상태로 시작한 세션은 null. "예약 진행"/새로고침 복원 시점에 로그인하면 채워 넣음.
    private String memberEmail;

    // WeddingPlan과 별개로 세션 시점에 입력받은 스냅샷 값 (모드 선택 시 필수 입력)
    private Long budget;

    // 정규화된 region 컬럼 대신 Company.address LIKE 매칭에 그대로 쓸 자유 텍스트로 저장
    // (기존 openAIClient.ChatService의 search_companies keyword 관례와 동일한 방식)
    private String region;

    private LocalDate weddingDate;

    // "빠르게" / "자세히" 중 어느 모드로 시작했는지
    @Column(length = 20)
    private String mode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "hall_status")),
            @AttributeOverride(name = "selectedCmno", column = @Column(name = "hall_selected_cmno")),
            @AttributeOverride(name = "note", column = @Column(name = "hall_note"))
    })
    @Builder.Default
    private SlotState hallSlot = SlotState.empty();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "studio_status")),
            @AttributeOverride(name = "selectedCmno", column = @Column(name = "studio_selected_cmno")),
            @AttributeOverride(name = "note", column = @Column(name = "studio_note"))
    })
    @Builder.Default
    private SlotState studioSlot = SlotState.empty();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "dress_status")),
            @AttributeOverride(name = "selectedCmno", column = @Column(name = "dress_selected_cmno")),
            @AttributeOverride(name = "note", column = @Column(name = "dress_note"))
    })
    @Builder.Default
    private SlotState dressSlot = SlotState.empty();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "makeup_status")),
            @AttributeOverride(name = "selectedCmno", column = @Column(name = "makeup_selected_cmno")),
            @AttributeOverride(name = "note", column = @Column(name = "makeup_note"))
    })
    @Builder.Default
    private SlotState makeupSlot = SlotState.empty();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void changeMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public void changeBudget(Long budget) {
        this.budget = budget;
        touch();
    }

    public void changeRegion(String region) {
        this.region = region;
        touch();
    }

    public void changeWeddingDate(LocalDate weddingDate) {
        this.weddingDate = weddingDate;
        touch();
    }

    public void changeMode(String mode) {
        this.mode = mode;
        touch();
    }

    public void changeHallSlot(SlotState hallSlot) {
        this.hallSlot = hallSlot;
        touch();
    }

    public void changeStudioSlot(SlotState studioSlot) {
        this.studioSlot = studioSlot;
        touch();
    }

    public void changeDressSlot(SlotState dressSlot) {
        this.dressSlot = dressSlot;
        touch();
    }

    public void changeMakeupSlot(SlotState makeupSlot) {
        this.makeupSlot = makeupSlot;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
