package com.wedding.quote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import com.wedding.company.domain.CompanyCategory;
import com.wedding.global.domain.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// AI 견적서 - 사용자가 업로드한 업체 견적서 사진 1장 = row 1개. 우열은 판단하지 않고(같은
// 카테고리끼리 비교할 때 가격/항목/조건 "차이점"만 뽑아줌), 업로드 시점에 AI가 뽑아낸 고정
// 스키마 필드만 저장한다. 30일 지나면 QuoteCleanupScheduler가 예외 없이 정리한다
// (AiPlanSession과 달리 "저장/즐겨찾기" 같은 예외 플래그가 없는 단순 flat TTL).
@Entity
@Table(name = "tbl_quote")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Quote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quoteId;

    @Column(nullable = false)
    private String memberEmail;

    @Column(nullable = false)
    private String imageFileName;

    // AI가 사진을 보고 HALL/STUDIO/DRESS/MAKEUP 중 하나로 분류 - 같은 카테고리끼리만 비교
    // 가능하게 하는 기준이라 자유 텍스트가 아니라 기존 CompanyCategory enum 그대로 씀.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyCategory category;

    private String vendorNameGuess;

    private Long totalPrice;

    private Long perGuestPrice;

    // List<QuoteItemDTO> 직렬화 - 항목 하나만 따로 쿼리할 일이 없어서 자식 테이블 대신 JSON으로 저장
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String itemsJson;

    // List<String> 직렬화 - 업로드 시점에 AI가 뽑은 "주의할 점"
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String hiddenNotesJson;
}
