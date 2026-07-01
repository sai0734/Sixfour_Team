package com.wedding.company.service;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyImage;
import com.wedding.company.domain.CompanyType;
import com.wedding.company.domain.DressDetail;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.dto.HallDetailDTO;
import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.dto.StudioDetailDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.StudioDetailRepository;
import com.wedding.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

  private final CompanyRepository companyRepository;
  private final HallDetailRepository hallDetailRepository;
  private final DressDetailRepository dressDetailRepository;
  private final MakeupDetailRepository makeupDetailRepository;
  private final StudioDetailRepository studioDetailRepository;

  @Override
  public PageResponseDTO<CompanyListDTO> getList(CompanySearchDTO searchDTO) {

    Pageable pageable = PageRequest.of(
        searchDTO.getPage() - 1,
        searchDTO.getSize(),
        getSort(searchDTO.getSort()));

    Page<Company> result = companyRepository.searchList(
        searchDTO.getType(),
        emptyToNull(searchDTO.getRegion()),
        emptyToNull(searchDTO.getKeyword()),
        searchDTO.getMinPrice(),
        searchDTO.getMaxPrice(),
        pageable);

    List<CompanyListDTO> dtoList = result.get()
        .map(this::entityToListDTO)
        .toList();

    return PageResponseDTO.<CompanyListDTO>withAll()
        .dtoList(dtoList)
        .totalCount(result.getTotalElements())
        .pageRequestDTO(searchDTO)
        .build();
  }

  @Override
  public Long register(CompanyDTO companyDTO) {

    Company company = dtoToEntity(companyDTO);
    Company savedCompany = companyRepository.save(company);

    saveDetail(savedCompany, companyDTO);

    return savedCompany.getCno();
  }

  @Override
  public CompanyDTO get(Long cno) {

    Company company = companyRepository.selectOne(cno).orElseThrow();

    return entityToDTO(company);
  }

  @Override
  public void modify(CompanyDTO companyDTO) {

    Company company = companyRepository.selectOne(companyDTO.getCno()).orElseThrow();

    company.changeBasic(
        companyDTO.getName(),
        companyDTO.getCeoName(),
        companyDTO.getType(),
        companyDTO.getRegion(),
        companyDTO.getAddress(),
        companyDTO.getLatitude(),
        companyDTO.getLongitude(),
        companyDTO.getPhone(),
        companyDTO.getPrice(),
        companyDTO.getDescription());

    company.clearImages();

    if (companyDTO.getUploadFileNames() != null) {
      companyDTO.getUploadFileNames().forEach(company::addImageString);
    }

    saveDetail(company, companyDTO);
  }

  @Override
  public void remove(Long cno) {
    companyRepository.updateToDelete(cno, true);
  }

  private Company dtoToEntity(CompanyDTO companyDTO) {

    Company company = Company.builder()
        .cno(companyDTO.getCno())
        .name(companyDTO.getName())
        .ceoName(companyDTO.getCeoName())
        .type(companyDTO.getType())
        .region(companyDTO.getRegion())
        .address(companyDTO.getAddress())
        .latitude(companyDTO.getLatitude())
        .longitude(companyDTO.getLongitude())
        .phone(companyDTO.getPhone())
        .price(companyDTO.getPrice())
        .description(companyDTO.getDescription())
        .ratingAvg(companyDTO.getRatingAvg())
        .reviewCount(companyDTO.getReviewCount())
        .viewCount(companyDTO.getViewCount())
        .delFlag(companyDTO.isDelFlag())
        .build();

    if (companyDTO.getUploadFileNames() != null) {
      companyDTO.getUploadFileNames().forEach(company::addImageString);
    }

    return company;
  }

  private CompanyListDTO entityToListDTO(Company company) {

    String mainImage = company.getImageList() == null || company.getImageList().isEmpty()
        ? null
        : company.getImageList().get(0).getFileName();

    return CompanyListDTO.builder()
        .cno(company.getCno())
        .name(company.getName())
        .type(company.getType())
        .region(company.getRegion())
        .address(company.getAddress())
        .latitude(company.getLatitude())
        .longitude(company.getLongitude())
        .price(company.getPrice())
        .description(company.getDescription())
        .ratingAvg(company.getRatingAvg())
        .reviewCount(company.getReviewCount())
        .mainImage(mainImage)
        .build();
  }

  private CompanyDTO entityToDTO(Company company) {

    CompanyDTO companyDTO = CompanyDTO.builder()
        .cno(company.getCno())
        .name(company.getName())
        .ceoName(company.getCeoName())
        .type(company.getType())
        .region(company.getRegion())
        .address(company.getAddress())
        .latitude(company.getLatitude())
        .longitude(company.getLongitude())
        .phone(company.getPhone())
        .price(company.getPrice())
        .description(company.getDescription())
        .ratingAvg(company.getRatingAvg())
        .reviewCount(company.getReviewCount())
        .viewCount(company.getViewCount())
        .delFlag(company.isDelFlag())
        .uploadFileNames(company.getImageList().stream()
            .map(CompanyImage::getFileName)
            .toList())
        .build();

    hallDetailRepository.findByCompany_Cno(company.getCno())
        .map(this::hallToDTO)
        .ifPresent(companyDTO::setHallDetail);

    dressDetailRepository.findByCompany_Cno(company.getCno())
        .map(this::dressToDTO)
        .ifPresent(companyDTO::setDressDetail);

    makeupDetailRepository.findByCompany_Cno(company.getCno())
        .map(this::makeupToDTO)
        .ifPresent(companyDTO::setMakeupDetail);

    studioDetailRepository.findByCompany_Cno(company.getCno())
        .map(this::studioToDTO)
        .ifPresent(companyDTO::setStudioDetail);

    return companyDTO;
  }

  private void saveDetail(Company company, CompanyDTO companyDTO) {

    if (company.getType() == CompanyType.HALL && companyDTO.getHallDetail() != null) {
      HallDetailDTO dto = companyDTO.getHallDetail();
      HallDetail detail = hallDetailRepository.findByCompany_Cno(company.getCno())
          .orElse(HallDetail.builder().company(company).build());
      detail.changeDetail(dto.getHallType(), dto.getRepresentative(), dto.getCapacity(), dto.getMealType(),
          dto.getParkingAvailable(), dto.getWeddingTime(), dto.getHallDescription(), dto.getImageUrl());
      hallDetailRepository.save(detail);
    }

    if (company.getType() == CompanyType.DRESS && companyDTO.getDressDetail() != null) {
      DressDetailDTO dto = companyDTO.getDressDetail();
      DressDetail detail = dressDetailRepository.findByCompany_Cno(company.getCno())
          .orElse(DressDetail.builder().company(company).build());
      detail.changeDetail(dto.getDressStyle(), dto.getFittingPrice(),
          dto.getPremiumLineAvailable(), dto.getRentalPeriod(), dto.getSizeRange());
      dressDetailRepository.save(detail);
    }

    if (company.getType() == CompanyType.MAKEUP && companyDTO.getMakeupDetail() != null) {
      MakeupDetailDTO dto = companyDTO.getMakeupDetail();
      MakeupDetail detail = makeupDetailRepository.findByCompany_Cno(company.getCno())
          .orElse(MakeupDetail.builder().company(company).build());
      detail.changeDetail(dto.getHairIncluded(), dto.getMakeupIncluded(), dto.getNailIncluded(),
          dto.getVisitAvailable(), dto.getMakeupStyle(), dto.getStaffInfo(),
          dto.getHairPrice(), dto.getMakeupPrice(), dto.getNailPrice());
      makeupDetailRepository.save(detail);
    }

    if (company.getType() == CompanyType.STUDIO && companyDTO.getStudioDetail() != null) {
      StudioDetailDTO dto = companyDTO.getStudioDetail();
      StudioDetail detail = studioDetailRepository.findByCompany_Cno(company.getCno())
          .orElse(StudioDetail.builder().company(company).build());
      detail.changeDetail(dto.getConcept(), dto.getThemeTags(), dto.getShootingHours(),
          dto.getOriginalProvided(), dto.getRetouchIncluded());
      studioDetailRepository.save(detail);
    }
  }

  private HallDetailDTO hallToDTO(HallDetail detail) {
    return HallDetailDTO.builder()
        .hdno(detail.getHdno())
        .hallType(detail.getHallType())
        .representative(detail.getRepresentative())
        .capacity(detail.getCapacity())
        .mealType(detail.getMealType())
        .parkingAvailable(detail.getParkingAvailable())
        .weddingTime(detail.getWeddingTime())
        .hallDescription(detail.getHallDescription())
        .imageUrl(detail.getImageUrl())
        .build();
  }

  private DressDetailDTO dressToDTO(DressDetail detail) {
    return DressDetailDTO.builder()
        .ddno(detail.getDdno())
        .dressStyle(detail.getDressStyle())
        .fittingPrice(detail.getFittingPrice())
        .premiumLineAvailable(detail.getPremiumLineAvailable())
        .rentalPeriod(detail.getRentalPeriod())
        .sizeRange(detail.getSizeRange())
        .build();
  }

  private MakeupDetailDTO makeupToDTO(MakeupDetail detail) {
    return MakeupDetailDTO.builder()
        .mdno(detail.getMdno())
        .hairIncluded(detail.getHairIncluded())
        .makeupIncluded(detail.getMakeupIncluded())
        .nailIncluded(detail.getNailIncluded())
        .visitAvailable(detail.getVisitAvailable())
        .makeupStyle(detail.getMakeupStyle())
        .staffInfo(detail.getStaffInfo())
        .hairPrice(detail.getHairPrice())
        .makeupPrice(detail.getMakeupPrice())
        .nailPrice(detail.getNailPrice())
        .build();
  }

  private StudioDetailDTO studioToDTO(StudioDetail detail) {
    return StudioDetailDTO.builder()
        .sdno(detail.getSdno())
        .concept(detail.getConcept())
        .themeTags(detail.getThemeTags())
        .shootingHours(detail.getShootingHours())
        .originalProvided(detail.getOriginalProvided())
        .retouchIncluded(detail.getRetouchIncluded())
        .build();
  }

  private Sort getSort(String sort) {
    if ("rating".equalsIgnoreCase(sort)) {
      return Sort.by("ratingAvg").descending();
    }
    if ("priceAsc".equalsIgnoreCase(sort)) {
      return Sort.by("price").ascending();
    }
    if ("priceDesc".equalsIgnoreCase(sort)) {
      return Sort.by("price").descending();
    }
    return Sort.by("cno").descending();
  }

  private String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
