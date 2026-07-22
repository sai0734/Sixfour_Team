package com.wedding.aidress.service;

import com.wedding.aidress.client.AiDressTryOnClient;
import com.wedding.aidress.client.OpenAiBackgroundClient;
import com.wedding.aidress.domain.MemberTryOnHistory;
import com.wedding.aidress.dto.AiDressBackgroundRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.dto.TryOnHistoryDTO;
import com.wedding.aidress.dto.TryOnHistoryUpdateDTO;
import com.wedding.aidress.repository.MemberTryOnHistoryRepository;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.global.util.CustomFileUtil;
import java.io.IOException;
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
  public AiDressTryOnResponseDTO tryOn(
      String memberEmail, Long dressItemId, MultipartFile personPhoto) {
    if (personPhoto == null || personPhoto.isEmpty()) {
      throw new IllegalArgumentException("합성할 내 사진이 없습니다.");
    }

    DressItem dressItem = dressItemRepository.findById(dressItemId)
        .orElseThrow(() -> new NoSuchElementException("드레스를 찾을 수 없습니다."));

    byte[] personBytes;
    try {
      personBytes = personPhoto.getBytes();
    } catch (IOException e) {
      throw new IllegalStateException("내 사진을 읽을 수 없습니다.", e);
    }

    // 사람 사진은 메모리만 사용 (upload/DB 저장 없음). 배경은 /apply-background
    String tryOnBase64 =
        aiDressTryOnClient.synthesize(
            personBytes,
            personPhoto.getOriginalFilename(),
            toPublicImageUrl(dressItem.getImageUrl()));

    return AiDressTryOnResponseDTO.builder()
        .resultImageUrl("data:image/png;base64," + tryOnBase64)
        .resultImageBase64(tryOnBase64)
        .dressItemId(dressItem.getDressItemId())
        .dressName(dressItem.getItemName())
        .backgroundPrompt(null)
        .build();
  }

  @Override
  public AiDressTryOnResponseDTO applyBackground(AiDressBackgroundRequestDTO requestDTO) {
    String prompt = Optional.ofNullable(requestDTO.getBackgroundPrompt())
        .map(String::trim)
        .filter(StringUtils::hasText)
        .orElseThrow(() -> new IllegalArgumentException("배경 프롬프트를 입력해 주세요."));

    String sourceBase64 = Optional.ofNullable(requestDTO.getImageBase64())
        .map(String::trim)
        .filter(StringUtils::hasText)
        .orElseThrow(() -> new IllegalArgumentException("배경을 적용할 이미지가 없습니다."));

    String resultBase64 =
        openAiBackgroundClient.applyBackgroundFromBase64(sourceBase64, prompt);

    return AiDressTryOnResponseDTO.builder()
        .resultImageUrl("data:image/png;base64," + resultBase64)
        .resultImageBase64(resultBase64)
        .backgroundPrompt(prompt)
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
    String newResultFileName;

    if (newPrompt != null) {
      throw new UnsupportedOperationException(
          "합성 결과는 서버에 저장되지 않습니다. 배경을 바꾸려면 다시 입어보기를 실행해 주세요.");
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

  private String toPublicImageUrl(String fileName) {
    if (fileName != null && (fileName.startsWith("http://") || fileName.startsWith("https://"))) {
      return fileName;
    }
    return serverHost + "/api/companies/images/view/" + fileName;
  }
}
