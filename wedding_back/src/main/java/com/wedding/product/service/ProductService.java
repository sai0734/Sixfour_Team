package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.dto.ProductDTO;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ProductService {

    PageResponseDTO<ProductDTO> getProductList(PageRequestDTO pageRequestDTO);

    ProductDTO getProduct(Long pno);

    Long productRegister(ProductDTO productDTO);

    Long productModify(ProductDTO productDTO);

    Long productRemove(Long pno);

}
