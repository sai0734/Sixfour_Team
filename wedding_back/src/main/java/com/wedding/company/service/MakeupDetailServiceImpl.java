package com.wedding.company.service;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.MakeupPackage;
import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.dto.MakeupPackageDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MakeupDetailServiceImpl implements MakeupDetailService {

  private final MakeupDetailRepository makeupDetailRepository;
  private final MakeupPackageRepository makeupPackageRepository;
  private final CompanyRepository companyRepository;

  @Override
  public MakeupDetailDTO get(Long cmno) {
    return makeupDetailRepository.findByCompany_Cmno(cmno)
        .map(this::toDTO)
        .orElse(null);
  }

  @Override
  @Transactional
  public void update(Long cmno, MakeupDetailDTO dto) {
    MakeupDetail detail = makeupDetailRepository.findByCompany_Cmno(cmno)
        .orElseGet(() -> {
          Company company = companyRepository.findById(cmno)
              .orElseThrow(() -> new EntityNotFoundException("Company not found: " + cmno));
          MakeupDetail newDetail = MakeupDetail.builder().company(company).build();
          return makeupDetailRepository.save(newDetail);
        });

    detail.change(
        dto.getIncludesHairService(),
        dto.getIncludesMakeupService(),
        dto.getIncludesNailService(),
        dto.getHairPrice(),
        dto.getMakeupPrice(),
        dto.getNailPrice()
    );

    makeupPackageRepository.deleteByCompany_Cmno(cmno);

    List<MakeupPackageDTO> pkgList = dto.getPackages() != null ? dto.getPackages() : new ArrayList<>();
    Company company = companyRepository.getReferenceById(cmno);

    for (MakeupPackageDTO p : pkgList) {
      if (p == null || p.getPackageType() == null) continue;
      makeupPackageRepository.save(
          MakeupPackage.builder()
              .company(company)
              .packageType(p.getPackageType())
              .discountRate(p.getDiscountRate())
              .build()
      );
    }
  }

  private MakeupDetailDTO toDTO(MakeupDetail detail) {
    List<MakeupPackageDTO> packages = makeupPackageRepository
        .findByCompany_Cmno(detail.getCmno())
        .stream()
        .map(pkg -> MakeupPackageDTO.builder()
            .packageId(pkg.getPackageId())
            .cmno(pkg.getCompany().getCmno())
            .packageType(pkg.getPackageType())
            .discountRate(pkg.getDiscountRate())
            .build())
        .toList();

    return MakeupDetailDTO.builder()
        .cmno(detail.getCmno())
        .includesHairService(detail.getIncludesHairService())
        .includesMakeupService(detail.getIncludesMakeupService())
        .includesNailService(detail.getIncludesNailService())
        .hairPrice(detail.getHairPrice())
        .makeupPrice(detail.getMakeupPrice())
        .nailPrice(detail.getNailPrice())
        .packages(packages)
        .build();
  }
}
