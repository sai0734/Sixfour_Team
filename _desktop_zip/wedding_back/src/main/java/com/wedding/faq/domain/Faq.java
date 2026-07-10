package com.wedding.faq.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_faq")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long faqId;

    // 예산 / 업체 / 일정 / 계약 / 기타
    private String category;

    @Lob
    private String question;

    // AI 생성 or 팀에서 직접 입력
    @Lob
    private String answer;

    @Builder.Default
    private int likeCount = 0;

    private int sortOrder;

    public void changeCategory(String category) {
        this.category = category;
    }

    public void changeQuestion(String question) {
        this.question = question;
    }

    public void changeAnswer(String answer) {
        this.answer = answer;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void increaseLikeCount() {
        this.likeCount = this.likeCount + 1;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

}
