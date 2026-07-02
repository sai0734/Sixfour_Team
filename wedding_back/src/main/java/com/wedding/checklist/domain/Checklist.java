package com.wedding.checklist.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_checklist")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checklistId;

    // Member.email을 FK로 참조 (Member PK 타입이 String이라 동일하게 맞춤)
    private String memberEmail;

    private String title;

    @Builder.Default
    private boolean isDone = false;

    private LocalDate dueDate;

    private int sortOrder;

    // 준비 단계 (1: 기본 계획, 2: 업체 계약, 3: 청첩장·답례품 등, 이름은 프론트에서 고정 매핑)
    private int stage;

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    public void changeDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void changeStage(int stage) {
        this.stage = stage;
    }

}
