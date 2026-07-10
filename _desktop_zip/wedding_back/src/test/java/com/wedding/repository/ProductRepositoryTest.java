package com.wedding.repository;

import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.repository.ProductRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@Log4j2
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void addProduct() {

        for(int i = 0; i < 10 ; i++) {
            Product product = Product.builder()
                    .pname("상품" + i)
                    .price(i * 1000)
                    .pdesc("내가바로 상품" + i + "번이야")
                    .category("상품")
                    .build();

            product.addImageString("IMAGE1.jpg");
            product.addImageString("IMAGE2.jpg");

            productRepository.save(product);
        }
    }

    @Test
    public void deleteProduct() {

        for(long i = 0; i < 100 ; i++) {
            productRepository.deleteById(i+1);
        }
    }

    @Test
    public void selectProductAll() {

        Pageable pageable = PageRequest.of(0,10, Sort.by("pno").descending());

        Page<Object[]> result = productRepository.selectProductList(pageable);

        result.getContent().forEach(arr -> {
            Product product = (Product) arr[0];
            ProductImage productImage = (ProductImage) arr[1];

            log.info("상품: " + product.getPname());
            log.info("대표이미지: " + productImage.getFileName());
            });
        }

    @Test
    public void selectProduct() {

        Long pno = 21L;

        Optional<Product> result = productRepository.selectProductOne(pno);

        Product product = result.orElseThrow();

        log.info(product.getPname());

    }

    @Test
    @Transactional
    public void modify() {

        Long pno = 21L;
        boolean flag = true;

        productRepository.softDelete(pno, flag);

        Optional<Product> result = productRepository.findById(pno);

        Product product = result.orElseThrow();

        product.changeDesc("끄아아 바뀐다아아아");
        product.changePrice(10000);

        log.info("상품: " + product.getPname());
        log.info("삭제상태: " + product.isDelFlag());
        log.info("가격:" + product.getPrice());
        log.info("정보:" + product.getPdesc());

    }

}
