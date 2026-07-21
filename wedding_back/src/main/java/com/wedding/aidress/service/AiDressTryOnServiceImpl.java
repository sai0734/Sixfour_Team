package com.wedding.aidress.service;

import com.wedding.aidress.client.AiDressTryOnClient;
import com.wedding.aidress.client.OpenAiBackgroundClient;
import com.wedding.aidress.domain.MemberTryOnHistory;
import com.wedding.aidress.domain.MemberTryOnPhoto;
import com.wedding.aidress.dto.AiDressTryOnRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.dto.TryOnHistoryDTO;
import com.wedding.aidress.dto.TryOnHistoryUpdateDTO;
import com.wedding.aidress.repository.MemberTryOnHistoryRepository;
import com.wedding.aidress.repository.MemberTryOnPhotoRepository;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.global.util.CustomFileUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AiDressTryOnServiceImpl implements AiDressTryOnService {

  private final DressItemRepository dressItemRepository;
  private final MemberTryOnPhotoRepository memberTryOnPhotoRepository;
  private final MemberTryOnHistoryRepository memberTryOnHistoryRepository;
  private final CustomFileUtil customFileUtil;
  private final AiDressTryOnClient aiDressTryOnClient;
  private final OpenAiBackgroundClient openAiBackgroundClient;

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
        .filter(StringUtils::hasText)
        .orElseGet(() -> getMyPhoto(memberEmail));

    if (!StringUtils.hasText(photoFileName)) {
      throw new IllegalArgumentException("합성할 내 사진이 없습니다.");
    }

    String tryOnUrl = callAiDressTryOnApi(photoFileName, dressItem.getImageUrl());
    String tryOnFileName = extractFileName(tryOnUrl);

    String backgroundPrompt = Optional.ofNullable(requestDTO.getBackgroundPrompt())
        .map(String::trim)
        .filter(StringUtils::hasText)
        .orElse(null);

    String resultUrl = tryOnUrl;
    if (backgroundPrompt != null) {
      resultUrl = openAiBackgroundClient.applyBackground(tryOnUrl, backgroundPrompt);
    }
    String resultFileName = extractFileName(resultUrl);

    MemberTryOnHistory history = memberTryOnHistoryRepository.save(
        MemberTryOnHistory.builder()
            .memberEmail(memberEmail)
            .dressItemId(dressItem.getDressItemId())
            .dressName(dressItem.getItemName())
            .tryOnFileName(tryOnFileName)
            .resultFileName(resultFileName)
            .backgroundPrompt(backgroundPrompt)
            .build());

    return AiDressTryOnResponseDTO.builder()
        .historyId(history.getHistoryId())
        .resultImageUrl(toPublicImageUrl(resultFileName))
        .tryOnImageUrl(toPublicImageUrl(tryOnFileName))
        .dressItemId(dressItem.getDressItemId())
        .dressName(dressItem.getItemName())
        .backgroundPrompt(backgroundPrompt)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TryOnHistoryDTO> getHistory(String memberEmail) {
    return memberTryOnHistoryRepository.findByMemberEmailOrderByHistoryIdDesc(memberEmail)
        .stream()
        .map(this::toHistoryDto)
        .toList();
  }

  @Override
  public TryOnHistoryDTO updateHistory(
      String memberEmail, Long historyId, TryOnHistoryUpdateDTO updateDTO) {
    MemberTryOnHistory history = getOwnedHistory(memberEmail, historyId);

    String newPrompt = Optional.ofNullable(updateDTO.getBackgroundPrompt())
        .map(String::trim)
        .filter(StringUtils::hasText)
        .orElse(null);

    String oldResultFileName = history.getResultFileName();
    String tryOnUrl = toPublicImageUrl(history.getTryOnFileName());
    String newResultFileName;

    if (newPrompt != null) {
      String newResultUrl = openAiBackgroundClient.applyBackground(tryOnUrl, newPrompt);
      newResultFileName = extractFileName(newResultUrl);
    } else {
      newResultFileName = history.getTryOnFileName();
    }

    history.changeBackground(newPrompt, newResultFileName);

    // 이전 배경 결과 파일이 CatVTON 원본과 다르면 삭제
    if (StringUtils.hasText(oldResultFileName)
        && !Objects.equals(oldResultFileName, history.getTryOnFileName())
        && !Objects.equals(oldResultFileName, newResultFileName)) {
      customFileUtil.deleteFiles(List.of(oldResultFileName));
    }

    return toHistoryDto(history);
  }

  @Override
  public void deleteHistory(String memberEmail, Long historyId) {
    MemberTryOnHistory history = getOwnedHistory(memberEmail, historyId);

    List<String> filesToDelete = new ArrayList<>();
    if (StringUtils.hasText(history.getTryOnFileName())) {
      filesToDelete.add(history.getTryOnFileName());
    }
    if (StringUtils.hasText(history.getResultFileName())
        && !Objects.equals(history.getResultFileName(), history.getTryOnFileName())) {
      filesToDelete.add(history.getResultFileName());
    }

    memberTryOnHistoryRepository.delete(history);
    customFileUtil.deleteFiles(filesToDelete);
  }

  private MemberTryOnHistory getOwnedHistory(String memberEmail, Long historyId) {
    MemberTryOnHistory history = memberTryOnHistoryRepository.findById(historyId)
        .orElseThrow(() -> new NoSuchElementException("합성 기록을 찾을 수 없습니다."));
    if (!Objects.equals(history.getMemberEmail(), memberEmail)) {
      throw new IllegalArgumentException("본인 합성 기록만 수정/삭제할 수 있습니다.");
    }
    return history;
  }

  private TryOnHistoryDTO toHistoryDto(MemberTryOnHistory h) {
    return TryOnHistoryDTO.builder()
        .historyId(h.getHistoryId())
        .dressItemId(h.getDressItemId())
        .dressName(h.getDressName())
        .resultImageUrl(toPublicImageUrl(h.getResultFileName()))
        .tryOnImageUrl(toPublicImageUrl(h.getTryOnFileName()))
        .backgroundPrompt(h.getBackgroundPrompt())
        .createdAt(h.getRegDate())
        .build();
  }

  private String callAiDressTryOnApi(String personFileName, String dressImageUrl) {
    String humanImgUrl = toPublicImageUrl(personFileName);
    String garmentImgUrl = toPublicImageUrl(dressImageUrl);
    return aiDressTryOnClient.synthesize(humanImgUrl, garmentImgUrl);
  }

  private String toPublicImageUrl(String fileName) {
    if (fileName != null && (fileName.startsWith("http://") || fileName.startsWith("https://"))) {
      return fileName;
    }
    return serverHost + "/api/companies/images/view/" + fileName;
  }

  private String extractFileName(String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
    }
    int viewIdx = imageUrl.indexOf("/view/");
    if (viewIdx >= 0) {
      return imageUrl.substring(viewIdx + "/view/".length());
    }
    int slash = imageUrl.lastIndexOf('/');
    return slash >= 0 ? imageUrl.substring(slash + 1) : imageUrl;
  }
}
