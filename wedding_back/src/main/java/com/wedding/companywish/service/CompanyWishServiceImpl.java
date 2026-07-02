package com.wedding.companywish.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.companywish.domain.CompanyWish;
import com.wedding.companywish.dto.CompanyWishDTO;
import com.wedding.companywish.repository.CompanyWishRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CompanyWishServiceImpl implements CompanyWishService {

    private final CompanyWishRepository companyWishRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(CompanyWishDTO companyWishDTO) {

        log.info("companyWish register.........");

        // 중복 찜 방지 (UNIQUE 제약이 있지만, 애플리케이션 레벨에서 먼저 체크해서 명확한 예외 던짐)
        boolean exists = companyWishRepository.existsByMemberEmailAndCmno(
                companyWishDTO.getMemberEmail(), companyWishDTO.getCmno());

        if (exists) {
            throw new IllegalStateException(
                    "이미 찜한 업체입니다. memberEmail: " + companyWishDTO.getMemberEmail()
                            + ", cmno: " + companyWishDTO.getCmno());
        }

        CompanyWish companyWish = modelMapper.map(companyWishDTO, CompanyWish.class);

        CompanyWish savedCompanyWish = companyWishRepository.save(companyWish);

        return savedCompanyWish.getWishId();
    }

    @Override
    public List<CompanyWishDTO> listByMember(String memberEmail) {

        List<CompanyWish> result = companyWishRepository.findByMemberEmail(memberEmail);

        return result.stream()
                .map(companyWish -> modelMapper.map(companyWish, CompanyWishDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void remove(Long wishId) {

        companyWishRepository.deleteById(wishId);
    }

    @Override
    public void removeByMemberAndCompany(String memberEmail, Long cmno) {

        Optional<CompanyWish> result = companyWishRepository.findByMemberEmailAndCmno(memberEmail, cmno);

        CompanyWish companyWish = result.orElseThrow();

        companyWishRepository.deleteById(companyWish.getWishId());
    }

}
