package com.wedding.aidress.service;

import com.wedding.aidress.client.AiDressTryOnClient;
import com.wedding.aidress.domain.MemberTryOnPhoto;
import com.wedding.aidress.dto.AiDressTryOnRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.repository.MemberTryOnPhotoRepository;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.global.util.CustomFileUtil;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AiDressTryOnServiceImpl implements AiDressTryOnService {

  private final DressItemRepository dressItemRepository;
  private final MemberTryOnPhotoRepository memberTryOnPhotoRepository;
  private final CustomFileUtil customFileUtil;
  private final AiDressTryOnClient aiDressTryOnClient;

  @Value("${com.wedding.server.host}")
  private String serverHost;

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<DressTryOnItemDTO> getDressList(PageRequestDTO pageRequestDTO) {
    Pageable pageable = PageRequest.of(
        pageRequestDTO.getPage() - 1,
        pageRequestDTO.getSize());

    Page<DressItem> result = dressItemRepository.findAllForTryOn(DressItemType.SUIT, pageable);

    List<DressTryOnItemDTO> dtoList = result.getContent().stream()
        .map(item -> DressTryOnItemDTO.builder()
            .dressItemId(item.getDressItemId())
            .cmno(item.getCompany().getCmno())
            .companyName(item.getCompany().getName())
            .itemName(item.getItemName())
            .price(item.getPrice())
            .imageUrl(item.getImageUrl())
            .itemType(item.getItemType())
            .sizeRange(item.getSizeRange())
            .build())
        .toList();

    return PageResponseDTO.<DressTryOnItemDTO>withAll()
        .dtoList(dtoList)
        .totalCount(result.getTotalElements())
        .pageRequestDTO(pageRequestDTO)
        .build();
  }

  @Override
  public String saveMyPhoto(String memberEmail, MultipartFile file) {
    List<String> saved = customFileUtil.saveFiles(List.of(file));
    if (saved == null || saved.isEmpty()) {
      throw new IllegalArgumentException("업로드할 사진이 없습니다.");
    }

    String fileName = saved.get(0);

    memberTryOnPhotoRepository.findById(memberEmail)
        .ifPresentOrElse(
            photo -> photo.changePhoto(fileName),
            () -> memberTryOnPhotoRepository.save(
                MemberTryOnPhoto.builder()
                    .memberEmail(memberEmail)
                    .photoFileName(fileName)
                    .build()));

    return fileName;
  }

  @Override
  @Transactional(readOnly = true)
  public String getMyPhoto(String memberEmail) {
    return memberTryOnPhotoRepository.findById(memberEmail)
        .map(MemberTryOnPhoto::getPhotoFileName)
        .orElse("");
  }

  @Override
  public AiDressTryOnResponseDTO tryOn(String memberEmail, AiDressTryOnRequestDTO requestDTO) {
    DressItem dressItem = dressItemRepository.findById(requestDTO.getDressItemId())
        .orElseThrow(() -> new NoSuchElementException("드레스를 찾을 수 없습니다."));

    String photoFileName = Optional.ofNullable(requestDTO.getPhotoFileName())
        .filter(name -> !name.isBlank())
        .orElseGet(() -> getMyPhoto(memberEmail));

    if (photoFileName.isBlank()) {
      throw new IllegalArgumentException("합성할 내 사진이 없습니다.");
    }

    String resultUrl = callAiDressTryOnApi(photoFileName, dressItem.getImageUrl());

    return AiDressTryOnResponseDTO.builder()
        .resultImageUrl(resultUrl)
        .dressItemId(dressItem.getDressItemId())
        .dressName(dressItem.getItemName())
        .build();
  }


  private String callAiDressTryOnApi(String personFileName, String dressImageUrl) {
    String humanImgUrl = toPublicImageUrl(personFileName);
    String garmentImgUrl = toPublicImageUrl(dressImageUrl);
    return aiDressTryOnClient.synthesize(humanImgUrl, garmentImgUrl);
  }

  private String toPublicImageUrl(String fileName) {
    return serverHost + "/api/companies/images/view/" + fileName;
  }
}
