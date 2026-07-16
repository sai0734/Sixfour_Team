package com.wedding.aiplan.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 결과 화면 카드 하나 (문서 5번: "예산 내 1,240만원 · 2.3km · '아늑한 분위기' 태그 일치" 같은 추천 근거 포함)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanPackageCandidateDTO {

    private Long pno;

    private String name;

    private String description;

    private BigDecimal packagePrice;

    private Double distanceKm;

    private Long hallCmno;
    private String hallName;

    private Long dressCmno;
    private String dressName;

    private Long studioCmno;
    private String studioName;

    private Long makeupCmno;
    private String makeupName;

    // "예산 내 1,240만원 · 2.3km" 같이 화면에 그대로 노출할 추천 근거 텍스트
    private String reason;

    // "PACKAGE"(기존 CompanyPackage 그대로) / "INDIVIDUAL_COMBO"(카테고리별 개별 업체를 조합)
    // / "AI_COMBO"(5단계 - AI가 자유 텍스트/취향까지 반영해서 고름)
    private String sourceType;

    // AI_COMBO일 때만 채워짐 - 카테고리별로 AI가 그 업체를 고른 이유(한 문장)
    private String hallReason;
    private String studioReason;
    private String dressReason;
    private String makeupReason;
}
