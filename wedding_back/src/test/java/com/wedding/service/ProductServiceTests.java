package com.wedding.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.dto.ProductDTO;
import com.wedding.product.service.ProductService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Log4j2
public class ProductServiceTests {

    @Autowired
    private ProductService productService;

    @Test
    public void testGetList() {

        PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
                .page(1)
                .size(10)
                .build();

        PageResponseDTO<ProductDTO> pageResponseDTO = productService.getProductList(pageRequestDTO);

        pageResponseDTO.getDtoList().forEach(dto -> log.info("상품 pno: " + dto.getPno() + "상품 이름: " + dto.getPname()));

    }

    @Test
    public void testGet() {
        Long pno = 31L;

        ProductDTO productDTO = productService.getProductOne(pno);

        log.info(productDTO.getPname());

    }

    @Test
    public void testRegister() {

        ProductDTO productDTO = ProductDTO.builder()
                .pname("테스트 상품")
                .price(88000)
                .pdesc("이거슨 테스트 상품이여")
                .category("DRESS")
                .stockQty(20)
                .uploadFileNames(List.of("register1.jpg", "register2.jpg"))
                .build();

        Long pno = productService.productRegister(productDTO);

        log.info("추가된 상품의 pno: " + pno);

    }

    @Test
    public void testModify() {

        Long pno = 31L;

        ProductDTO productDTO = ProductDTO.builder()
                .pno(pno)
                .pname("수정된 상품")
                .price(55000)
                .pdesc("이거슨 수정된 상품이여")
                .category("STUDIO")
                .stockQty(7)
                .uploadFileNames(List.of("modify1.jpg", "modify2.jpg"))
                .build();

        productService.productModify(productDTO);

        log.info("수정된 상품 pno: " + pno);

    }

    @Test
    public void testRemove() {

        Long pno = 32L;

        productService.productRemove(pno);

        log.info("삭제된 상품: " + pno);

    }

}