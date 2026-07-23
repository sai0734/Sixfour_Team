package com.wedding.quote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import com.wedding.global.domain.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// 견적서 비교 결과 - "비교하기" 누를 때마다 하나씩 쌓인다(AI웨딩플랜 히스토리 배지와 같은 개념).
// 원본 Quote가 나중에 30일 TTL로 삭제돼도 이 기록은 그대로 남아야 해서, quoteId 참조가 아니라
// 비교 시점의 QuoteDTO 스냅샷을 통째로 JSON으로 저장해둔다(비교 자체를 다시 계산 안 하고
// 그대로 다시 보여줄 수 있게).
@Entity
@Table(name = "tbl_quote_comparison")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuoteComparison extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comparisonId;

    @Column(nullable = false)
    private String memberEmail;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String quoteASnapshotJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String quoteBSnapshotJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String priceDifference;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String onlyInAJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String onlyInBJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String conditionDifferencesJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String commonNotesJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String suggestedQuestionsJson;
}
