package com.wedding.companywish.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.company.domain.Company;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.companywish.domain.CompanyWish;
import com.wedding.companywish.dto.CompanyWishItemDTO;
import com.wedding.companywish.repository.CompanyWishRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class CompanyWishServiceImpl implements CompanyWishService {

    private final CompanyWishRepository companyWishRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean check(String memberEmail, Long cmno) {
        return companyWishRepository.existsByMemberEmailAndCmno(memberEmail, cmno);
    }

    @Override
    public void add(String memberEmail, Long cmno) {
        addWithOption(memberEmail, cmno, "", 0, null);
    }

    @Override
    public void remove(String memberEmail, Long cmno) {
        companyWishRepository.deleteByMemberEmailAndCmnoAndOptionName(memberEmail, cmno, "");
        log.info("업체 찜 해제 완료. memberEmail={}, cmno={}", memberEmail, cmno);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDTO> getMyCompanies(String memberEmail) {

        List<CompanyWish> wishes = companyWishRepository.findByMemberEmailOrderByWishIdDesc(memberEmail);

        return wishes.stream()
                .map(wish -> companyRepository.findById(wish.getCmno()).orElse(null))
                .filter(company -> company != null && !company.isDelFlag())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ↓↓↓ 재원 추가 - 옵션과 함께 찜하기

    @Override
    public void addWithOption(String memberEmail, Long cmno, String optionName, int optionAmount, String optionImage) {
        String normalized = optionName == null ? "" : optionName;

        var existing = companyWishRepository
                .findByMemberEmailAndCmnoAndOptionName(memberEmail, cmno, normalized);

        if (existing.isPresent()) {
            CompanyWish wish = existing.get();
            wish.changeOptionInfo(optionAmount, optionImage);
            companyWishRepository.save(wish);
            log.info("기존 찜 옵션 가격 갱신. memberEmail={}, cmno={}, optionName={}, optionAmount={}",
                    memberEmail, cmno, normalized, optionAmount);
            return;
        }

        CompanyWish companyWish = CompanyWish.builder()
                .memberEmail(memberEmail)
                .cmno(cmno)
                .optionName(normalized)
                .optionAmount(optionAmount)
                .optionImage(optionImage)
                .build();

        companyWishRepository.save(companyWish);
        log.info("업체 찜 등록 완료. memberEmail={}, cmno={}, optionName={}, optionAmount={}",
                memberEmail, cmno, normalized, optionAmount);
    }

    @Override
    public void removeByWishId(String memberEmail, Long wishId) {
        companyWishRepository.deleteByWishIdAndMemberEmail(wishId, memberEmail);
        log.info("업체 찜 해제 완료(wishId). memberEmail={}, wishId={}", memberEmail, wishId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyWishItemDTO> getMyCompanyWishItems(String memberEmail) {

        List<CompanyWish> wishes = companyWishRepository.findByMemberEmailOrderByWishIdDesc(memberEmail);

        return wishes.stream()
                .map(wish -> {
                    Company company = companyRepository.findById(wish.getCmno()).orElse(null);
                    if (company == null || company.isDelFlag()) {
                        return null;
                    }

                    List<String> fileNames = company.getImageList().stream()
                            .map(img -> img.getFileName())
                            .collect(Collectors.toList());

                    return CompanyWishItemDTO.builder()
                            .wishId(wish.getWishId())
                            .cmno(wish.getCmno())
                            .optionName(wish.getOptionName())
                            .optionAmount(wish.getOptionAmount())
                            .optionImage(wish.getOptionImage())
                            .regDate(wish.getRegDate())
                            .category(company.getCategory() != null ? company.getCategory().name() : null)
                            .name(company.getName())
                            .address(company.getAddress())
                            .phone(company.getPhone())
                            .priceAvg(company.getPriceAvg() != null ? company.getPriceAvg().intValue() : null)
                            .uploadFileNames(fileNames)
                            .build();
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }
    // ↑↑↑ 재원 추가

    private CompanyDTO toDTO(Company company) {
        List<String> fileNames = company.getImageList().stream()
                .map(img -> img.getFileName())
                .collect(Collectors.toList());

        return CompanyDTO.builder()
                .cmno(company.getCmno())
                .category(company.getCategory())
                .name(company.getName())
                .ceoName(company.getCeoName())
                .phone(company.getPhone())
                .address(company.getAddress())
                .latitude(company.getLatitude())
                .longitude(company.getLongitude())
                .description(company.getDescription())
                .priceAvg(company.getPriceAvg())
                .uploadFileNames(fileNames)
                .build();
    }
}
