package com.wedding.company.service;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.DressDetail;
import com.wedding.company.domain.DressItem;
import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.dto.DressItemDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.DressItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DressDetailServiceImpl implements DressDetailService {

  private final CompanyRepository companyRepository;
  private final DressDetailRepository dressDetailRepository;
  private final DressItemRepository dressItemRepository;

  @Override
  public DressDetailDTO get(Long cmno) {
    return dressDetailRepository.findByCompany_Cmno(cmno)
        .map(this::toDTO)
        .orElse(null);
  }

  @Override
  @Transactional
  public void update(Long cmno, DressDetailDTO dto) {
    DressDetail detail = dressDetailRepository.findByCompany_Cmno(cmno)
        .orElseGet(() -> {
          Company company = companyRepository.findById(cmno)
              .orElseThrow(() -> new EntityNotFoundException("Company not found: " + cmno));
          return dressDetailRepository.save(
              DressDetail.builder().company(company).build());
        });

    detail.change(dto.getSizeRange());

    dressItemRepository.deleteByCompany_Cmno(cmno);

    List<DressItemDTO> itemList = dto.getItems() != null ? dto.getItems() : new ArrayList<>();
    Company company = companyRepository.getReferenceById(cmno);

    for (int i = 0; i < itemList.size(); i++) {
      DressItemDTO it = itemList.get(i);
      if (it == null || it.getItemType() == null) continue;
      dressItemRepository.save(DressItem.builder()
          .company(company)
          .itemName(it.getItemName())
          .price(it.getPrice())
          .imageUrl(it.getImageUrl())
          .ord(it.getOrd() != null ? it.getOrd() : i)
          .itemType(it.getItemType())
          .styleTags(it.getStyleTags())
          .sizeRange(it.getSizeRange())
          .build());
    }
  }

  private DressDetailDTO toDTO(DressDetail detail) {
    List<DressItemDTO> items = dressItemRepository
        .findByCompany_CmnoOrderByOrdAsc(detail.getCmno())
        .stream()
        .map(item -> DressItemDTO.builder()
            .dressItemId(item.getDressItemId())
            .cmno(item.getCompany().getCmno())
            .itemName(item.getItemName())
            .price(item.getPrice())
            .imageUrl(item.getImageUrl())
            .ord(item.getOrd())
            .itemType(item.getItemType())
            .styleTags(item.getStyleTags())
            .sizeRange(item.getSizeRange())
            .build())
        .toList();

    return DressDetailDTO.builder()
        .cmno(detail.getCmno())
        .sizeRange(detail.getSizeRange())
        .items(items)
        .build();
  }
}
