package com.wedding.company.service;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.CompanyImage;
import com.wedding.company.domain.DressDetail;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.MakeupPackage;
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.dto.DressItemDTO;
import com.wedding.company.dto.HallDetailDTO;
import com.wedding.company.dto.HallItemDTO;
import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.dto.MakeupPackageDTO;
import com.wedding.company.dto.StudioDetailDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.HallItemRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import com.wedding.company.repository.StudioDetailRepository;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

  private final CompanyRepository companyRepository;
  private final HallDetailRepository hallDetailRepository;
  private final HallItemRepository hallItemRepository;
  private final DressDetailRepository dressDetailRepository;
  private final DressItemRepository dressItemRepository;
  private final MakeupDetailRepository makeupDetailRepository;
  private final MakeupPackageRepository makeupPackageRepository;
  private final StudioDetailRepository studioDetailRepository;
  private final MemberRepository memberRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<CompanyListDTO> getList(CompanySearchDTO searchDTO) {
    Pageable pageable = PageRequest.of(
            searchDTO.getPage() - 1,
            searchDTO.getSize(),
            getSort(searchDTO.getSort()));

    Page<Company> result = companyRepository.searchList(
            searchDTO.getCategory(),
            blankToNull(searchDTO.getKeyword()),
            searchDTO.getMinPrice(),
            searchDTO.getMaxPrice(),
            pageable);

    List<CompanyListDTO> dtoList = result.get().map(this::toListDTO).toList();

    return PageResponseDTO.<CompanyListDTO>withAll()
            .dtoList(dtoList)
            .totalCount(result.getTotalElements())
            .pageRequestDTO(searchDTO)
            .build();
  }

  @Override
  public Long register(CompanyDTO companyDTO) {
    Company company = companyRepository.save(toEntity(companyDTO));
    saveDetails(company, companyDTO);
    return company.getCmno();
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyDTO get(Long cmno) {
    Company company = companyRepository.selectOne(cmno).orElseThrow();
    return toDTO(company);
  }

  @Override
  public void modify(CompanyDTO companyDTO) {
    Company company = companyRepository.selectOne(companyDTO.getCmno()).orElseThrow();

    company.change(
            companyDTO.getName(),
            companyDTO.getCeoName(),
            companyDTO.getCategory(),
            companyDTO.getPhone(),
            companyDTO.getAddress(),
            companyDTO.getLatitude(),
            companyDTO.getLongitude(),
            companyDTO.getDescription(),
            companyDTO.getPriceAvg());

    company.clearImages();
    if (companyDTO.getUploadFileNames() != null) {
      companyDTO.getUploadFileNames().forEach(company::addImage);
    }

    saveDetails(company, companyDTO);
  }

  @Override
  public void remove(Long cmno) {
    companyRepository.updateDelFlag(cmno, true);
  }

  @Override
  public void assignManager(Long cmno, String managerEmail) {
    // 한 회원은 업체 1곳만 담당 - 이 회원이 이미 다른 업체를 담당 중이면 그쪽부터 해제
    // (이 회원은 새 업체를 담당하게 되므로 MANAGER 권한은 유지, 이전 업체만 해제)
    companyRepository.findByManagerEmail(managerEmail)
            .filter(existing -> !existing.getCmno().equals(cmno))
            .ifPresent(Company::unassignManager);

    Company company = companyRepository.selectOne(cmno).orElseThrow();

    // 이 업체를 이미 담당하던 다른 회원이 있다면, 그 회원은 더 이상 담당 업체가 없으므로 MANAGER 권한 해제
    String previousManagerEmail = company.getManagerEmail();
    if (previousManagerEmail != null && !previousManagerEmail.equals(managerEmail)) {
      downgradeIfNoOtherCompany(previousManagerEmail);
    }

    company.assignManager(managerEmail);

    // 새로 임명된 회원에게 MANAGER 권한 부여 (기존에 이 부분이 누락되어 DB에 권한이 반영되지 않았음)
    grantManagerRole(managerEmail);
  }

  @Override
  public void unassignManager(Long cmno) {
    Company company = companyRepository.selectOne(cmno).orElseThrow();
    String managerEmail = company.getManagerEmail();
    company.unassignManager();

    if (managerEmail != null) {
      downgradeIfNoOtherCompany(managerEmail);
    }
  }

  // 해당 회원에게 MANAGER 권한을 부여한다.
  // 매니저는 "업체 문의 답변" 기능만 쓸 수 있어야 하므로, 일반 회원(USER) 권한은 함께 회수한다.
  // (ADMIN이 매니저로 겸직하는 경우는 굳이 USER를 뺏지 않음 - 관리자 기능 보호 차원)
  private void grantManagerRole(String managerEmail) {
    Member member = memberRepository.getWithRoles(managerEmail);
    if (member == null) {
      throw new NoSuchElementException("해당 회원을 찾을 수 없습니다: " + managerEmail);
    }
    member.addRole(MemberRole.MANAGER);
    if (!member.getMemberRoleList().contains(MemberRole.ADMIN)) {
      member.removeRole(MemberRole.USER);
    }
    memberRepository.save(member);
  }

  // 해당 회원이 더 이상 담당 중인 업체가 없다면 MANAGER 권한을 회수하고, 일반 회원(USER) 권한을 되돌려준다
  private void downgradeIfNoOtherCompany(String managerEmail) {
    boolean stillManaging = companyRepository.findByManagerEmail(managerEmail).isPresent();
    if (!stillManaging) {
      Member member = memberRepository.getWithRoles(managerEmail);
      if (member != null) {
        member.removeRole(MemberRole.MANAGER);
        member.addRole(MemberRole.USER);
        memberRepository.save(member);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyDTO getManagedCompany(String memberEmail) {
    return companyRepository.findByManagerEmail(memberEmail)
            .map(this::toDTO)
            .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompanyDTO> getManagedCompanies() {
    return companyRepository.findByManagerEmailIsNotNull()
            .stream()
            .map(this::toDTO)
            .toList();
  }

  private Company toEntity(CompanyDTO dto) {
    Company company = Company.builder()
            .cmno(dto.getCmno())
            .category(dto.getCategory())
            .name(dto.getName())
            .ceoName(dto.getCeoName())
            .phone(dto.getPhone())
            .address(dto.getAddress())
            .latitude(dto.getLatitude())
            .longitude(dto.getLongitude())
            .description(dto.getDescription())
            .priceAvg(dto.getPriceAvg())
            .delFlag(dto.isDelFlag())
            .build();

    if (dto.getUploadFileNames() != null) {
      dto.getUploadFileNames().forEach(company::addImage);
    }
    return company;
  }

  private CompanyListDTO toListDTO(Company company) {
    return CompanyListDTO.builder()
            .cmno(company.getCmno())
            .category(company.getCategory())
            .name(company.getName())
            .phone(company.getPhone())
            .address(company.getAddress())
            .latitude(company.getLatitude())
            .longitude(company.getLongitude())
            .description(company.getDescription())
            .priceAvg(company.getPriceAvg())
            .mainImage(firstImage(company))
            .build();
  }

  private CompanyDTO toDTO(Company company) {
    CompanyDTO dto = CompanyDTO.builder()
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
            .delFlag(company.isDelFlag())
            .managerEmail(company.getManagerEmail())
            .uploadFileNames(company.getImageList().stream().map(CompanyImage::getFileName).toList())
            .build();

    hallDetailRepository.findByCompany_Cmno(company.getCmno()).map(this::toHallDTO).ifPresent(dto::setHallDetail);
    dressDetailRepository.findByCompany_Cmno(company.getCmno()).map(this::toDressDTO).ifPresent(dto::setDressDetail);
    makeupDetailRepository.findByCompany_Cmno(company.getCmno()).map(this::toMakeupDTO).ifPresent(dto::setMakeupDetail);
    studioDetailRepository.findByCompany_Cmno(company.getCmno()).map(this::toStudioDTO).ifPresent(dto::setStudioDetail);

    return dto;
  }

  private void saveDetails(Company company, CompanyDTO dto) {
    if (company.getCategory() == CompanyCategory.HALL && dto.getHallDetail() != null) {
      saveHall(company, dto.getHallDetail());
    }
    if (company.getCategory() == CompanyCategory.DRESS && dto.getDressDetail() != null) {
      saveDress(company, dto.getDressDetail());
    }
    if (company.getCategory() == CompanyCategory.MAKEUP && dto.getMakeupDetail() != null) {
      saveMakeup(company, dto.getMakeupDetail());
    }
    if (company.getCategory() == CompanyCategory.STUDIO && dto.getStudioDetail() != null) {
      saveStudio(company, dto.getStudioDetail());
    }
  }

  private void saveHall(Company company, HallDetailDTO dto) {
    HallDetail detail = hallDetailRepository.findByCompany_Cmno(company.getCmno())
            .orElse(HallDetail.builder().company(company).build());
    detail.change(dto.getHallName(), dto.getAddress(), dto.getLatitude(), dto.getLongitude(), dto.getPhone(),
            dto.getRepresentative(), dto.getHallType(), dto.getDescription(), dto.getImageUrl());
    hallDetailRepository.save(detail);

    hallItemRepository.deleteByCompany_Cmno(company.getCmno());
    if (dto.getItems() != null) {
      dto.getItems().stream().map(item -> toHallItem(company, item)).forEach(hallItemRepository::save);
    }
  }

  private void saveDress(Company company, DressDetailDTO dto) {
    DressDetail detail = dressDetailRepository.findByCompany_Cmno(company.getCmno())
            .orElse(DressDetail.builder().company(company).build());
    detail.change(dto.getSizeRange());
    dressDetailRepository.save(detail);

    dressItemRepository.deleteByCompany_Cmno(company.getCmno());
    if (dto.getItems() != null) {
      dto.getItems().stream().map(item -> toDressItem(company, item)).forEach(dressItemRepository::save);
    }
  }

  private void saveMakeup(Company company, MakeupDetailDTO dto) {
    MakeupDetail detail = makeupDetailRepository.findByCompany_Cmno(company.getCmno())
            .orElse(MakeupDetail.builder().company(company).build());
    detail.change(dto.getIncludesHairService(), dto.getIncludesMakeupService(), dto.getIncludesNailService(),
            dto.getHairPrice(), dto.getMakeupPrice(), dto.getNailPrice());
    makeupDetailRepository.save(detail);

    makeupPackageRepository.deleteByCompany_Cmno(company.getCmno());
    if (dto.getPackages() != null) {
      dto.getPackages().stream().map(pkg -> toMakeupPackage(company, pkg)).forEach(makeupPackageRepository::save);
    }
  }

  private void saveStudio(Company company, StudioDetailDTO dto) {
    StudioDetail detail = studioDetailRepository.findByCompany_Cmno(company.getCmno())
            .orElse(StudioDetail.builder().company(company).build());
    detail.change(dto.getThemeTags());
    studioDetailRepository.save(detail);
  }

  private HallDetailDTO toHallDTO(HallDetail detail) {
    return HallDetailDTO.builder()
            .cmno(detail.getCmno())
            .hallName(detail.getHallName())
            .address(detail.getAddress())
            .latitude(detail.getLatitude())
            .longitude(detail.getLongitude())
            .phone(detail.getPhone())
            .representative(detail.getRepresentative())
            .hallType(detail.getHallType())
            .description(detail.getDescription())
            .imageUrl(detail.getImageUrl())
            .items(hallItemRepository.findByCompany_CmnoOrderByOrdAsc(detail.getCmno()).stream().map(this::toHallItemDTO).toList())
            .build();
  }

  private DressDetailDTO toDressDTO(DressDetail detail) {
    return DressDetailDTO.builder()
            .cmno(detail.getCmno())
            .sizeRange(detail.getSizeRange())
            .items(dressItemRepository.findByCompany_CmnoOrderByOrdAsc(detail.getCmno()).stream().map(this::toDressItemDTO).toList())
            .build();
  }

  private MakeupDetailDTO toMakeupDTO(MakeupDetail detail) {
    return MakeupDetailDTO.builder()
            .cmno(detail.getCmno())
            .includesHairService(detail.getIncludesHairService())
            .includesMakeupService(detail.getIncludesMakeupService())
            .includesNailService(detail.getIncludesNailService())
            .hairPrice(detail.getHairPrice())
            .makeupPrice(detail.getMakeupPrice())
            .nailPrice(detail.getNailPrice())
            .packages(makeupPackageRepository.findByCompany_Cmno(detail.getCmno()).stream().map(this::toMakeupPackageDTO).toList())
            .build();
  }

  private StudioDetailDTO toStudioDTO(StudioDetail detail) {
    return StudioDetailDTO.builder().cmno(detail.getCmno()).themeTags(detail.getThemeTags()).build();
  }

  private HallItem toHallItem(Company company, HallItemDTO dto) {
    return HallItem.builder()
            .hallItemId(dto.getHallItemId())
            .company(company)
            .itemName(dto.getItemName())
            .price(dto.getPrice())
            .capacity(dto.getCapacity())
            .imageUrl(dto.getImageUrl())
            .ord(dto.getOrd())
            .mealType(dto.getMealType())
            .build();
  }

  private HallItemDTO toHallItemDTO(HallItem item) {
    return HallItemDTO.builder()
            .hallItemId(item.getHallItemId())
            .cmno(item.getCompany().getCmno())
            .itemName(item.getItemName())
            .price(item.getPrice())
            .capacity(item.getCapacity())
            .imageUrl(item.getImageUrl())
            .ord(item.getOrd())
            .mealType(item.getMealType())
            .build();
  }

  private DressItem toDressItem(Company company, DressItemDTO dto) {
    return DressItem.builder()
            .dressItemId(dto.getDressItemId())
            .company(company)
            .itemName(dto.getItemName())
            .price(dto.getPrice())
            .imageUrl(dto.getImageUrl())
            .ord(dto.getOrd())
            .itemType(dto.getItemType())
            .styleTags(dto.getStyleTags())
            .sizeRange(dto.getSizeRange())
            .build();
  }

  private DressItemDTO toDressItemDTO(DressItem item) {
    return DressItemDTO.builder()
            .dressItemId(item.getDressItemId())
            .cmno(item.getCompany().getCmno())
            .itemName(item.getItemName())
            .price(item.getPrice())
            .imageUrl(item.getImageUrl())
            .ord(item.getOrd())
            .itemType(item.getItemType())
            .styleTags(item.getStyleTags())
            .sizeRange(item.getSizeRange())
            .build();
  }

  private MakeupPackage toMakeupPackage(Company company, MakeupPackageDTO dto) {
    return MakeupPackage.builder()
            .packageId(dto.getPackageId())
            .company(company)
            .packageType(dto.getPackageType())
            .discountRate(dto.getDiscountRate())
            .build();
  }

  private MakeupPackageDTO toMakeupPackageDTO(MakeupPackage pkg) {
    return MakeupPackageDTO.builder()
            .packageId(pkg.getPackageId())
            .cmno(pkg.getCompany().getCmno())
            .packageType(pkg.getPackageType())
            .discountRate(pkg.getDiscountRate())
            .build();
  }

  private String firstImage(Company company) {
    if (company.getImageList() == null || company.getImageList().isEmpty()) {
      return null;
    }
    return company.getImageList().get(0).getFileName();
  }

  private Sort getSort(String sort) {
    if ("priceAsc".equalsIgnoreCase(sort)) {
      return Sort.by("priceAvg").ascending();
    }
    if ("priceDesc".equalsIgnoreCase(sort)) {
      return Sort.by("priceAvg").descending();
    }
    return Sort.by("cmno").descending();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}