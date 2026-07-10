package com.wedding.product.service;

import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductOption;
import com.wedding.product.dto.ProductOptionDTO;
import com.wedding.product.repository.ProductOptionRepository;
import com.wedding.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProductOptionServiceImpl implements ProductOptionService{

    private final ProductOptionRepository productOptionRepository;

    private final ProductRepository productRepository;

    // 상품 옵션 리스트 조회
    @Override
    public List<ProductOptionDTO> listOption(Long pno) {

        List<ProductOption> productOptions = productOptionRepository.listOption(pno);

        List<ProductOptionDTO> productOptionsDTOList = productOptions.stream().map(option -> ProductOptionDTO.builder()
                .pono(option.getPono())
                .pno(pno)
                .optionName(option.getOptionName())
                .optionValue(option.getOptionValue())
                .extraPrice(option.getExtraPrice())
                .build()).collect(Collectors.toList());

        return productOptionsDTOList;
    }

    // 상품 옵션 등록
    @Override
    public Long register(ProductOptionDTO productOptionDTO) {

        Product product = Product.builder().pno(productOptionDTO.getPno()).build();

        ProductOption productOption = ProductOption.builder()
                .product(product)
                .optionName(productOptionDTO.getOptionName())
                .optionValue(productOptionDTO.getOptionValue())
                .extraPrice(productOptionDTO.getExtraPrice())
                .build();

        Long pono = productOptionRepository.save(productOption).getPono();

        return pono;
    }

    // 상품 옵션 수정
    @Override
    public Long modify(ProductOptionDTO productOptionDTO) {

        Optional<ProductOption> result = productOptionRepository.findOption(productOptionDTO.getPono(), productOptionDTO.getPno());

        ProductOption productOption = result.orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. pono=" + productOptionDTO.getPono()));

        productOption.changeOptionName(productOptionDTO.getOptionName());
        productOption.changeOptionValue(productOptionDTO.getOptionValue());
        productOption.changeOptionExtraPrice(productOptionDTO.getExtraPrice());

        Long pono = productOptionRepository.save(productOption).getPono();

        return pono;

    }

    // 상품 옵션 삭제
    @Override
    public Long remove(Long pno, Long pono) {

        ProductOption productOption = productOptionRepository.findOption(pono, pno)
                .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다. pono=" + pono));

        productOptionRepository.deleteById(pono);

        return pono;
    }

    // 옵션 선택 및 수량으로 값 매기기
    @Override
    public int calculatePrice(Long pno, Long pono, int qty) {

        Product product = productRepository.findById(pno)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 상품입니다. pno=" + pno));

        int extraPrice = 0;

        if (pono != null) {
            ProductOption productOption = productOptionRepository.findOption(pono, pno)
                    .orElseThrow(() ->new NoSuchElementException("옵션이 존재하지 않습니다. pono=" + pono));

            extraPrice = productOption.getExtraPrice();
        }

        int result = (product.getPrice() + extraPrice) * qty;

        return result;

    }

}
