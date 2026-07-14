package com.wedding.companywish.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.company.domain.Company;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.companywish.domain.CompanyWish;
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
        if (companyWishRepository.existsByMemberEmailAndCmno(memberEmail, cmno)) {
            log.info("이미 찜한 업체입니다. memberEmail={}, cmno={}", memberEmail, cmno);
            return;
        }

        CompanyWish companyWish = CompanyWish.builder()
                .memberEmail(memberEmail)
                .cmno(cmno)
                .build();

        companyWishRepository.save(companyWish);
        log.info("업체 찜 등록 완료. memberEmail={}, cmno={}", memberEmail, cmno);
    }

    @Override
    public void remove(String memberEmail, Long cmno) {
        companyWishRepository.deleteByMemberEmailAndCmno(memberEmail, cmno);
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
