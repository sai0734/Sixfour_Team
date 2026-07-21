package com.wedding.home.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.wedding.checkout.repository.OrderItemRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.CompanyImage;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.home.dto.MainHighlightsDTO;
import com.wedding.home.dto.MainHighlightsDTO.CompanyHighlight;
import com.wedding.home.dto.MainHighlightsDTO.ProductHighlight;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.repository.ProductRepository;
import com.wedding.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;

// 메인 화면 비로그인 폴라로이드 3장 - 홀/스드메 매출 1위 업체, 답례품 구매건수 1위 상품을 실데이터로 채워준다.
// 아직 실적이 하나도 없으면(신규 배포 직후 등) 해당 필드를 null로 내려서 프론트가 기본 문구를 보여주게 한다.
@Service
@RequiredArgsConstructor
public class HomeHighlightServiceImpl implements HomeHighlightService {

    private static final List<CompanyCategory> STYLING_CATEGORIES =
            List.of(CompanyCategory.DRESS, CompanyCategory.STUDIO, CompanyCategory.MAKEUP);

    private final ReservationRepository reservationRepository;
    private final OrderItemRepository orderItemRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

    @Override
    public MainHighlightsDTO getMainHighlights() {
        return MainHighlightsDTO.builder()
                .hallCompany(topCompany(reservationRepository
                        .findTopCompanyByCategory(CompanyCategory.HALL, PageRequest.of(0, 1))))
                .stylingCompany(topCompany(reservationRepository
                        .findTopCompanyByCategories(STYLING_CATEGORIES, PageRequest.of(0, 1))))
                .topProduct(topProduct(orderItemRepository.findTopPurchasedProduct(PageRequest.of(0, 1))))
                .build();
    }

    // row: [cmno, name, totalAmount]
    private CompanyHighlight topCompany(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        Long cmno = (Long) row[0];
        String name = (String) row[1];

        return CompanyHighlight.builder()
                .cmno(cmno)
                .name(name)
                .imageUrl(firstCompanyImage(cmno))
                .build();
    }

    // row: [pno, pname, totalQty]
    private ProductHighlight topProduct(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        Long pno = (Long) row[0];
        String name = (String) row[1];

        return ProductHighlight.builder()
                .pno(pno)
                .name(name)
                .imageUrl(firstProductImage(pno))
                .build();
    }

    private String firstCompanyImage(Long cmno) {
        Optional<Company> company = companyRepository.selectOne(cmno);
        return company.map(Company::getImageList)
                .filter(images -> !images.isEmpty())
                .flatMap(images -> images.stream().min(Comparator.comparingInt(CompanyImage::getOrd)))
                .map(CompanyImage::getFileName)
                .orElse(null);
    }

    private String firstProductImage(Long pno) {
        Optional<Product> product = productRepository.selectProductOne(pno);
        return product.map(Product::getImageList)
                .filter(images -> !images.isEmpty())
                .flatMap(images -> images.stream().min(Comparator.comparingInt(ProductImage::getOrd)))
                .map(ProductImage::getFileName)
                .orElse(null);
    }
}
