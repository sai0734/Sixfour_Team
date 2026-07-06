package com.wedding.product.service;

import com.wedding.product.dto.ProductOptionDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ProductOptionService {

    // 상품 옵션 리스트 조회하기
    List<ProductOptionDTO> listOption(Long pno);

    // 상품 옵션 등록
    Long register(ProductOptionDTO productOptionDTO);

    // 상품 옵션 수정
    Long modify(ProductOptionDTO productOptionDTO);

    // 상품 옵션 삭제
    Long remove(Long pno, Long pono);

    // 옵션 선택 및 수량으로 값 매기긱
    int calculatePrice(Long pno, Long pono, int qty);
}
