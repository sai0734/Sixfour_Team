package com.wedding.company.service;

import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.WeddingPackage;
import com.wedding.company.domain.WeddingPackageItem;
import com.wedding.company.dto.WeddingPackageDTO;
import com.wedding.company.dto.WeddingPackageItemDTO;
import com.wedding.company.dto.WeddingPackageListDTO;
import com.wedding.company.dto.WeddingPackageSearchDTO;
import com.wedding.company.repository.WeddingPackageRepository;
import com.wedding.global.dto.PageResponseDTO;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WeddingPackageServiceImpl implements WeddingPackageService {

    private final WeddingPackageRepository weddingPackageRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WeddingPackageListDTO> getList(WeddingPackageSearchDTO searchDTO) {
        Pageable pageable = PageRequest.of(
                searchDTO.getPage() - 1,
                searchDTO.getSize(),
                getSort(searchDTO.getSort()));

        Page<WeddingPackage> result = weddingPackageRepository.searchList(
                blankToNull(searchDTO.getKeyword()),
                pageable);

        List<WeddingPackageListDTO> dtoList = result.get().map(this::toListDTO).toList();

        return PageResponseDTO.<WeddingPackageListDTO>withAll()
                .dtoList(dtoList)
                .totalCount(result.getTotalElements())
                .pageRequestDTO(searchDTO)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WeddingPackageDTO get(Long weddingPackageId) {
        WeddingPackage pkg = weddingPackageRepository.selectOne(weddingPackageId)
                .orElseThrow(() -> new NoSuchElementException("패키지를 찾을 수 없습니다."));
        return toDTO(pkg);
    }

    @Override
    public Long register(WeddingPackageDTO dto) {
        WeddingPackage pkg = WeddingPackage.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .totalPrice(dto.getTotalPrice())
                .salePrice(dto.getSalePrice())
                .thumbnail(dto.getThumbnail())
                .build();

        saveItems(pkg, dto.getItems());
        return weddingPackageRepository.save(pkg).getWeddingPackageId();
    }

    @Override
    public void modify(WeddingPackageDTO dto) {
        WeddingPackage pkg = weddingPackageRepository.findById(dto.getWeddingPackageId())
                .orElseThrow(() -> new NoSuchElementException("패키지를 찾을 수 없습니다."));

        pkg.change(dto.getName(), dto.getDescription(), dto.getTotalPrice(),
                dto.getSalePrice(), dto.getThumbnail());

        pkg.clearItems();
        saveItems(pkg, dto.getItems());
    }

    @Override
    public void remove(Long weddingPackageId) {
        WeddingPackage pkg = weddingPackageRepository.findById(weddingPackageId)
                .orElseThrow(() -> new NoSuchElementException("패키지를 찾을 수 없습니다."));
        pkg.remove();
    }

    private void saveItems(WeddingPackage pkg, List<WeddingPackageItemDTO> items) {
        if (items == null) return;

        int order = 0;
        for (WeddingPackageItemDTO itemDTO : items) {
            if (itemDTO == null || itemDTO.getCategory() == null) continue;

            pkg.addItem(WeddingPackageItem.builder()
                    .category(itemDTO.getCategory())
                    .cmno(itemDTO.getCmno())
                    .companyName(itemDTO.getCompanyName())
                    .optionLabel(itemDTO.getOptionLabel())
                    .price(itemDTO.getPrice())
                    .sortOrder(itemDTO.getSortOrder() != null ? itemDTO.getSortOrder() : order++)
                    .build());
        }
    }

    private WeddingPackageListDTO toListDTO(WeddingPackage pkg) {
        return WeddingPackageListDTO.builder()
                .weddingPackageId(pkg.getWeddingPackageId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .totalPrice(pkg.getTotalPrice())
                .salePrice(pkg.getSalePrice())
                .thumbnail(pkg.getThumbnail())
                .itemCount(pkg.getItems() != null ? pkg.getItems().size() : 0)
                .regDate(pkg.getRegDate())
                .build();
    }

    private WeddingPackageDTO toDTO(WeddingPackage pkg) {
        return WeddingPackageDTO.builder()
                .weddingPackageId(pkg.getWeddingPackageId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .totalPrice(pkg.getTotalPrice())
                .salePrice(pkg.getSalePrice())
                .thumbnail(pkg.getThumbnail())
                .regDate(pkg.getRegDate())
                .items(pkg.getItems().stream().map(this::toItemDTO).toList())
                .build();
    }

    private WeddingPackageItemDTO toItemDTO(WeddingPackageItem item) {
        return WeddingPackageItemDTO.builder()
                .itemId(item.getItemId())
                .category(item.getCategory())
                .cmno(item.getCmno())
                .companyName(item.getCompanyName())
                .optionLabel(item.getOptionLabel())
                .price(item.getPrice())
                .sortOrder(item.getSortOrder())
                .build();
    }

    private Sort getSort(String sort) {
        if ("name".equals(sort)) {
            return Sort.by("name").ascending();
        }
        if ("price".equals(sort)) {
            return Sort.by("salePrice").ascending();
        }
        return Sort.by("weddingPackageId").descending();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}