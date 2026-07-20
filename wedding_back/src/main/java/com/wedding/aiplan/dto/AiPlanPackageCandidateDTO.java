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

    // 업체명(hallName)과 별개로, 그 업체 안에서 구체적으로 어떤 홀/연회장인지 - HallDetail.hallName
    // (예: 업체는 "토브헤세드", 이 값은 "플로렌스홀")
    private String hallRoomName;

    private Long dressCmno;
    private String dressName;

    // 5단계 - 드레스는 업체가 아니라 "구체적인 옵션(아이템)" 하나가 추천 대상. 상세페이지에서
    // 이 옵션을 바로 가리키는 딥링크를 만들 때 씀 (프론트 쪽 상세페이지가 이 id를 실제로
    // 활용하려면 별도 확인 필요 - 지금은 값만 내려줌).
    private Long dressItemId;

    // 업체명(dressName)과 별개로, 구체적으로 어떤 옵션(아이템)인지 - DressItem.itemName
    private String dressOptionName;

    private Long studioCmno;
    private String studioName;

    private Long makeupCmno;
    private String makeupName;

    // 사용자가 고른 메이크업 패키지 타입(MakeupPackageType 이름, 예: "FULL") - 취향과 정확히
    // 일치하는 업체를 찾았을 때만 채워짐. 프론트가 이 값으로 그 업체의 실제 옵션가(예약 페이지와
    // 동일한 계산)를 다시 계산해 makeupPrice 대신 보여준다. null이면 makeupPrice(업체 평균가)를 씀.
    private String makeupPackageType;

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

    // "PENDING"|"CONFIRMED"|"EXCLUDED" (SlotStatus.name()) - 프론트에서 확정 버튼 렌더링용
    private String hallStatus;
    private String studioStatus;
    private String dressStatus;
    private String makeupStatus;

    // 카드에 보여줄 대표 이미지 - 파일명만 담음(프론트에서 getCompanyImageUrl로 완성된 URL 만듦).
    // 홀/스튜디오/메이크업은 업체 대표 이미지, 드레스는 업체가 아니라 "옵션(드레스 아이템)" 이미지.
    private String hallImageUrl;
    private String studioImageUrl;
    private String dressImageUrl;
    private String makeupImageUrl;

    // 카테고리별 가격(Company.priceAvg 기준, 참고용) - packagePrice(합계)만 보여주다가
    // "홀은 얼마, 드레스는 얼마"를 따로 보고 싶다는 요청으로 추가.
    private BigDecimal hallPrice;
    private BigDecimal studioPrice;
    private BigDecimal dressPrice;
    private BigDecimal makeupPrice;
}
