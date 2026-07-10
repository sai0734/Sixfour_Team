package com.wedding.product.controller;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.global.util.CustomFileUtil;
import com.wedding.product.dto.ProductDTO;
import com.wedding.product.dto.ProductSearchDTO;
import com.wedding.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final CustomFileUtil customFileUtil;

//    // 상품 전체 리스트 조회하기 (대표이미지 1개, 삭제안된 상품)
//    @GetMapping("/list")
//    public PageResponseDTO<ProductDTO> list(PageRequestDTO pageRequestDTO) {
//
//        log.info("ProductController_list 실행~~~~~~~~");
//
//        PageResponseDTO<ProductDTO> pageResponseDTO = productService.getProductList(pageRequestDTO);
//
//        return pageResponseDTO;
//    }

    // 상품 1개 조회하기
    @GetMapping("/{pno}")
    public ProductDTO read(@PathVariable(name="pno") Long pno) {

        log.info("ProductController_read 실행~~~~~~~~");

        ProductDTO productDTO = productService.getProductOne(pno);

        return productDTO;
    }

    // 상품 등록
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/")
    public Map<String, Long> register(ProductDTO productDTO) {

        log.info("ProductController_register 실행~~~~~~~~");

        List<MultipartFile> files = productDTO.getFiles();
        List<String> uploadFileNames = customFileUtil.saveFiles(files);

        productDTO.setUploadFileNames(uploadFileNames);

        Long pno = productService.productRegister(productDTO);

        return Map.of("result", pno);

    }

    // 상품 수정
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{pno}")
    public Map<String, String> modify(@PathVariable(name="pno") Long pno, ProductDTO productDTO) {

        log.info("ProductController_modify 실행~~~~~~~~");

        productDTO.setPno(pno);

        // 기존 DB에 저장된 파일 목록
        ProductDTO oldProductDTO = productService.getProductOne(pno);
        List<String> oldFileNames = oldProductDTO.getUploadFileNames();

        // 새로 업로드된 파일 저장
        List<MultipartFile> files = productDTO.getFiles();
        List<String> currentUploadFileNames = customFileUtil.saveFiles(files);

        // 화면에 유지하는 파일(프론트에서 그대로 넘어온 uploadFileNames)
        List<String> uploadFileNames = productDTO.getUploadFileNames();
        if(uploadFileNames == null) {
            uploadFileNames = new ArrayList<>();
        }

        // 유지된 파일 + 새로 업로드 파일 = 최종 저장 목록
        if(currentUploadFileNames != null && currentUploadFileNames.size() > 0) {
            uploadFileNames.addAll(currentUploadFileNames);
        }
        productDTO.setUploadFileNames(uploadFileNames);

        // DB 수정
        productService.productModify(productDTO);

        // 최종 목록에 없는 기존 파일 골라서 삭제
        if(oldFileNames != null && oldFileNames.size() > 0) {
            List<String> uploadedFileNamesFinal = uploadFileNames;
            List<String> removeFiles = oldFileNames.stream()
                    .filter(fileName -> uploadedFileNamesFinal.indexOf(fileName) == -1)
                    .collect(Collectors.toList());

            customFileUtil.deleteFiles(removeFiles);

        }
        return  Map.of("RESULT", "SUCCESS");

    }

    // 상품 삭제(소프트 삭제)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{pno}")
    public Map<String, String> remove(@PathVariable(name = "pno") Long pno) {

        log.info("ProductController_remove 실행~~~~~~~~");

        productService.productRemove(pno);

        return Map.of("RESULT", "SUCCESS");

    }

    // 이미지 조회
    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName) {

        log.info("ProductController_viewFile 실행~~~~~~~~");

        return customFileUtil.getFile(fileName);
    }

    // 상품 전체 리스트 조회하기 (카테고리/가격/평점/검색어 필터 지원)
    @GetMapping("/list")
    public PageResponseDTO<ProductDTO> list(ProductSearchDTO productSearchDTO) {

        log.info("ProductController_list 실행~~~~~~~~");

        return productService.getProductList(productSearchDTO);
    }

    // 카테고리 목록 조회하기
    @GetMapping("/categories")
    public List<String> categories() {

        log.info("ProductController_categories 실행~~~~~~~~");

        return productService.getCategoryList();
    }

}