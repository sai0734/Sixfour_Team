package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.dto.AdminProductListDTO;
import com.wedding.product.dto.AdminProductSearchDTO;
import com.wedding.product.dto.ProductDTO;
import com.wedding.product.dto.ProductSearchDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ProductService {

    // 상품 전체 리스트 조회하기 (대표이미지 1개, 삭제안된 상품)
    PageResponseDTO<ProductDTO> getProductList(PageRequestDTO pageRequestDTO);

    // 상품 1개 조회하기
    ProductDTO getProductOne(Long pno);

    // 상품 등록
    Long productRegister(ProductDTO productDTO);

    // 상품 수정
    Long productModify(ProductDTO productDTO);

    // 상품 삭제
    Long productRemove(Long pno);

    // 상품 전체 리스트 조회하기 (카테고리/가격/평점/검색어 필터 지원)
    PageResponseDTO<ProductDTO> getProductList(ProductSearchDTO productSearchDTO);

    // 카테고리 목록 조회하기
    List<String> getCategoryList();

    // 관리자용 상품 리스트 조회
    PageResponseDTO<AdminProductListDTO> getAdminProductList(AdminProductSearchDTO searchDTO);

}
