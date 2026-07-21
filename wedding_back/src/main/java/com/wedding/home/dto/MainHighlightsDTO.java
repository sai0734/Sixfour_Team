package com.wedding.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 메인 화면 비로그인 폴라로이드 3장에 들어갈 실제 데이터.
// 아직 결제완료 실적이 하나도 없는 카테고리/상품은 해당 필드가 null로 내려가고, 프론트는 그때 기본 문구를 보여준다.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MainHighlightsDTO {

    private CompanyHighlight hallCompany;

    private CompanyHighlight stylingCompany; // 드레스·스튜디오·메이크업(스드메) 통합 매출 1위

    private ProductHighlight topProduct;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyHighlight {
        private Long cmno;
        private String name;
        private String imageUrl; // 파일명만 - 프론트가 getCompanyImageUrl()로 전체 URL 조립
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductHighlight {
        private Long pno;
        private String name;
        private String imageUrl; // 파일명만 - 프론트가 /api/product/view/{fileName}로 조립
    }
}
