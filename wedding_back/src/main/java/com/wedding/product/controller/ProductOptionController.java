package com.wedding.product.controller;

import com.wedding.product.dto.ProductOptionDTO;
import com.wedding.product.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductOptionController {

    private final ProductOptionService productOptionService;

    // 상품 옵션 리스트 조회
    @GetMapping("/{pno}/options")
    public List<ProductOptionDTO> listOption(@PathVariable Long pno) {

        log.info("ProductOptionController_listOption 실행~~~~~~~~");

        List<ProductOptionDTO> productOptionsDTO = productOptionService.listOption(pno);

        return productOptionsDTO;

    }

    // 상품 옵션 등록
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{pno}/options")
    public Map<String, Long> register(@PathVariable Long pno, @RequestBody ProductOptionDTO productOptionDTO) {

        log.info("ProductOptionController_register 실행~~~~~~~~");

        productOptionDTO.setPno(pno);

        Long pono = productOptionService.register(productOptionDTO);

        return Map.of("result", pono);

    }

    // 상품 옵션 수정
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{pno}/options/{pono}")
    public Map<String, Long> modify(@PathVariable Long pno, @PathVariable Long pono,
    @RequestBody ProductOptionDTO productOptionDTO) {

        log.info("ProductOptionController_modify 실행~~~~~~~~");

        productOptionDTO.setPno(pno);
        productOptionDTO.setPono(pono);

        Long result = productOptionService.modify(productOptionDTO);

        return Map.of("pono", result);

    }

    // 상품 옵션 삭제
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{pno}/options/{pono}")
    public Map<String, Long> remove(@PathVariable Long pno, @PathVariable Long pono) {

        log.info("ProductOptionController_remove 실행~~~~~~~~");

        Long result = productOptionService.remove(pno, pono);  // pno도 넘김

        return Map.of("pono", result);
    }

}
