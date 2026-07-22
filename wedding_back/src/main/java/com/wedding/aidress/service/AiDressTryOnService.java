package com.wedding.aidress.service;

import com.wedding.aidress.dto.AiDressBackgroundRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.dto.TryOnHistoryDTO;
import com.wedding.aidress.dto.TryOnHistoryUpdateDTO;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AiDressTryOnService {

  PageResponseDTO<DressTryOnItemDTO> getDressList(PageRequestDTO pageRequestDTO);

  /** 내 사진은 메모리로만 전달 (upload/DB 저장 없음) */
  AiDressTryOnResponseDTO tryOn(String memberEmail, Long dressItemId, MultipartFile personPhoto);

  /** 합성 결과에 배경만 적용 (upload 저장 없음) */
  AiDressTryOnResponseDTO applyBackground(AiDressBackgroundRequestDTO requestDTO);

  List<TryOnHistoryDTO> getHistory(String memberEmail);

  TryOnHistoryDTO updateHistory(
      String memberEmail, Long historyId, TryOnHistoryUpdateDTO updateDTO);

  void deleteHistory(String memberEmail, Long historyId);
}
