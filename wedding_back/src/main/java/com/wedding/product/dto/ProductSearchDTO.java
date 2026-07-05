package com.wedding.product.dto;

import com.wedding.global.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductSearchDTO extends PageRequestDTO {

    // 카테고리 필터 (여러 개 동시 선택 가능, null/빈 리스트면 전체)
    private List<String> categories;

    // 검색어 (상품명/설명, null이면 전체)
    private String keyword;

    // 가격 범위 (null이면 제한 없음)
    private Integer minPrice;
    private Integer maxPrice;

    // 평점 기준 (null이면 제한 없음, 있으면 이 값 이상만)
    private Double minRating;

    // 정렬 기준: popular(인기순) | latest(최신순, 기본값) | priceAsc(가격낮은순) | reviews(리뷰많은순)
    private String sortType;

}