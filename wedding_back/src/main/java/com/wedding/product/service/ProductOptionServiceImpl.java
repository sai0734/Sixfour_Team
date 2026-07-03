//package com.wedding.product.service;
//
//import com.wedding.product.domain.Product;
//import com.wedding.product.domain.ProductOption;
//import com.wedding.product.dto.ProductOptionDTO;
//import com.wedding.product.repository.ProductOptionRepository;
//import com.wedding.product.repository.ProductRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.log4j.Log4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@Log4j
//@RequiredArgsConstructor
//public class ProductOptionServiceImpl implements ProductService{
//
//    private final ProductOptionRepository productOptionRepository;
//
//    @Override
//    public List<ProductOptionDTO> listOption(Long pno) {
//
//        List<ProductOption> productOptions = productOptionRepository.listOption(pno);
//
//        List<ProductOptionDTO> productOptionDTOS = productOptions.stream().map(option -> ProductOptionDTO.builder()
//                .pono(productOptions.getLast().getPono())
//                .pno(pno)
//                .optionName(option.getOptionName())
//                .optionValue(option.getOptionValue())
//                .extraPrice(option.getExtraPrice())
//                .build()).collect(Collectors.toList());
//
//
//
//    }
//
//
//
//
//}
