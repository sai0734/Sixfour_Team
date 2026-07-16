package com.wedding.aiplan.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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

// 리파인 대화 한 턴이 끝날 때마다 그 시점의 AiPlanSession 슬롯 상태를 스냅샷으로 남겨서
// "이전 추천으로 되돌리기"를 지원함. 조회 빈도가 낮고 구조 조회가 필요 없어서
// (되돌릴 때 그대로 복원만 하면 됨) 슬롯 전체를 JSON 문자열 하나로 저장.
@Entity
@Table(name = "tbl_ai_plan_session_history")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanSessionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    // AiPlanSession.sessionId 참조 (FK 매핑 대신 값으로만 보관 - Checklist/Budget이
    // memberEmail을 String으로 직접 들고 있는 것과 동일한 관례)
    private Long sessionId;

    // 몇 번째 턴의 스냅샷인지 (0부터 시작, 되돌리기 목록 정렬/표시용)
    private int turnNo;

    // 그 시점 사용자 발화 원문 ("스튜디오 빼줘" 등). 초기 생성 시점 스냅샷은 null.
    @Column(length = 500)
    private String userMessage;

    // hallSlot/studioSlot/dressSlot/makeupSlot 4개 상태를 통째로 담은 JSON 스냅샷
    @Column(length = 2000)
    private String slotSnapshotJson;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}
