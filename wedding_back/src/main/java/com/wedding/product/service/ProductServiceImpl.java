package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.dto.ProductDTO;
import com.wedding.product.dto.ProductSearchDTO;
import com.wedding.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // 상품 전체 리스트 조회하기 (대표이미지 1개, 삭제안된 상품) - 검색조건 없이 새 메서드로 위임
    @Override
    public PageResponseDTO<ProductDTO> getProductList(PageRequestDTO pageRequestDTO) {

        ProductSearchDTO searchDTO = ProductSearchDTO.builder()
                .page(pageRequestDTO.getPage())
                .size(pageRequestDTO.getSize())
                .build();

        return getProductList(searchDTO);
    }

    // 상품 전체 리스트 조회하기 (카테고리/가격/평점/검색어/정렬 지원)
    @Override
    public PageResponseDTO<ProductDTO> getProductList(ProductSearchDTO productSearchDTO) {

        Sort sort = resolveSort(productSearchDTO.getSortType());

        Pageable pageable = PageRequest.of(
                productSearchDTO.getPage()-1,
                productSearchDTO.getSize(),
                sort);

        // 빈 리스트는 SQL의 "IN ()" 오류를 막기 위해 null로 취급
        List<String> categories = productSearchDTO.getCategories();
        if (categories != null && categories.isEmpty()) {
            categories = null;
        }

        Page<Object[]> result = productRepository.searchProductList(
                categories,
                productSearchDTO.getKeyword(),
                productSearchDTO.getMinPrice(),
                productSearchDTO.getMaxPrice(),
                productSearchDTO.getMinRating(),
                pageable);

        List<ProductDTO> dtoList = result.get().map(arr -> {

            Product product = (Product) arr[0];
            ProductImage productImage = (ProductImage) arr[1];

            ProductDTO productDTO = ProductDTO.builder()
                    .pno(product.getPno())
                    .pname(product.getPname())
                    .price(product.getPrice())
                    .pdesc(product.getPdesc())
                    .delFlag(product.isDelFlag())
                    .category(product.getCategory())
                    .stockQty(product.getStockQty())
                    .ratingAvg(product.getRatingAvg())
                    .reviewCount(product.getReviewCount())
                    .viewCount(product.getViewCount())
                    .salesCount(product.getSalesCount())
                    .build();

            if(productImage != null) {
                productDTO.setUploadFileNames(List.of(productImage.getFileName()));
            }

            return productDTO;

        }).collect(Collectors.toList());

        long totalCount = result.getTotalElements();

        return PageResponseDTO.<ProductDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(productSearchDTO)
                .totalCount(totalCount)
                .build();

    }

    // 정렬 옵션 문자열을 실제 Sort 객체로 변환
    private Sort resolveSort(String sortType) {

        if (sortType == null) {
            return Sort.by("pno").descending(); // 기본값: 최신순
        }

        switch (sortType) {
            case "popular":
                return Sort.by("salesCount").descending();
            case "priceAsc":
                return Sort.by("price").ascending();
            case "priceDesc":
                return Sort.by("price").descending();
            case "reviews":
                return Sort.by("reviewCount").descending();
            case "latest":
            default:
                return Sort.by("pno").descending();
        }
    }

    // 카테고리 목록 조회하기
    @Override
    public List<String> getCategoryList() {
        return productRepository.findDistinctCategories();
    }

    // 상품 1개 조회하기
    @Override
    public ProductDTO getProductOne(Long pno) {

        Optional<Product> result = productRepository.selectProductOne(pno);

        Product product = result.orElseThrow(() -> new NoSuchElementException("존재하지 않는 상품입니다. pno=" + pno));

        List<String> fileNames = product.getImageList().stream()
                .map(img -> img.getFileName()).collect(Collectors.toList());

        ProductDTO productDTO = ProductDTO.builder()
                .pno(product.getPno())
                .pname(product.getPname())
                .price(product.getPrice())
                .pdesc(product.getPdesc())
                .delFlag(product.isDelFlag())
                .category(product.getCategory())
                .stockQty(product.getStockQty())
                .ratingAvg(product.getRatingAvg())
                .reviewCount(product.getReviewCount())
                .viewCount(product.getViewCount())
                .salesCount(product.getSalesCount())
                .uploadFileNames(fileNames)
                .build();

        return productDTO;

    }

    // 상품 등록
    @Override
    public Long productRegister(ProductDTO productDTO) {

        Product product = Product.builder()
                .pname(productDTO.getPname())
                .price(productDTO.getPrice())
                .pdesc(productDTO.getPdesc())
                .category(productDTO.getCategory())
                .stockQty(productDTO.getStockQty())
                .build();

        List<String> uploadFileNames = productDTO.getUploadFileNames();

        if(uploadFileNames != null) {
            uploadFileNames.forEach(fileName -> product.addImageString(fileName));
        }

        Product productResult = productRepository.save(product);

        return productResult.getPno();

    }

    // 상품 수정
    @Override
    public Long productModify(ProductDTO productDTO) {

        Long pno = productDTO.getPno();

        Optional<Product> result = productRepository.selectProductOne(pno);

        Product product = result.orElseThrow(() -> new NoSuchElementException("존재하지 않는 상품입니다. pno=" + pno));

        List<String> uploadFileNames = productDTO.getUploadFileNames();
        if(uploadFileNames != null) {
            product.clearList();
            uploadFileNames.forEach(fileName -> product.addImageString(fileName));
        }

        product.changeName(productDTO.getPname());
        product.changePrice(productDTO.getPrice());
        product.changeDesc(productDTO.getPdesc());
        product.changeCategory(productDTO.getCategory());
        product.changeStockQty(productDTO.getStockQty());

        Product productResult = productRepository.save(product);

        return productResult.getPno();

    }

    // 상품 삭제
    @Override
    public Long productRemove(Long pno) {

        productRepository.softDelete(pno, true);

        return pno;

    }

}