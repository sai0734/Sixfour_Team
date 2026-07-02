package com.wedding.product.service;

import com.wedding.product.domain.ProductOption;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ProductOptionService {

    List<ProductOption> listOptions();

    Long register(ProductOption productOption);

    Long modify(ProductOption productOption);

    Long remove(Long pono);

    int calculatePrice(Long pno, Long pono, int qty);
}
