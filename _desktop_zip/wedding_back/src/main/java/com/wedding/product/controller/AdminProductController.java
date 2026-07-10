package com.wedding.product.controller;

import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.dto.AdminProductListDTO;
import com.wedding.product.dto.AdminProductSearchDTO;
import com.wedding.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponseDTO<AdminProductListDTO> list(AdminProductSearchDTO searchDTO) {

        log.info("AdminProductController_list 실행~~~~~~~~");

        return productService.getAdminProductList(searchDTO);
    }

}
