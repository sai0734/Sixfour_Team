package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.dto.ProductDTO;
import com.wedding.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public PageResponseDTO<ProductDTO> getProductList(PageRequestDTO pageRequestDTO) {

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage()-1,
                pageRequestDTO.getSize(),
                Sort.by("pno").descending());

        Page<Object[]> result = productRepository.selectProductList(pageable);

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

            String imageStr = productImage.getFileName();
            productDTO.setUploadFileNames(List.of(imageStr));

            return productDTO;
        }).collect(Collectors.toList());





        return null;
    }

    @Override
    public ProductDTO getProduct(Long pno) {
        return null;
    }

    @Override
    public Long productRegister(ProductDTO productDTO) {
        return null;
    }

    @Override
    public Long productModify(ProductDTO productDTO) {
        return null;
    }

    @Override
    public Long productRemove(Long pno) {
        return null;
    }

}
