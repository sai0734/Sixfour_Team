package com.wedding.aidress.controller;

import com.wedding.aidress.dto.AiDressBackgroundRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.dto.TryOnHistoryDTO;
import com.wedding.aidress.dto.TryOnHistoryUpdateDTO;
import com.wedding.aidress.service.AiDressTryOnService;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-dress")
public class AiDressTryOnController {

  private final AiDressTryOnService aiDressTryOnService;

  @GetMapping("/dresses")
  public PageResponseDTO<DressTryOnItemDTO> dresses(PageRequestDTO pageRequestDTO) {
    return aiDressTryOnService.getDressList(pageRequestDTO);
  }

  /** 내 사진 + 드레스 합성. 사람 사진은 upload에 저장하지 않음 */
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping(value = "/try-on", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AiDressTryOnResponseDTO tryOn(
      Principal principal,
      @RequestParam("dressItemId") Long dressItemId,
      @RequestPart("file") MultipartFile file) {
    return aiDressTryOnService.tryOn(principal.getName(), dressItemId, file);
  }

  /** 이미 합성된 이미지에 배경만 적용 (기록은 프론트에서 드레스 합성 직후 바로 표시) */
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/apply-background")
  public AiDressTryOnResponseDTO applyBackground(
      @RequestBody AiDressBackgroundRequestDTO requestDTO) {
    return aiDressTryOnService.applyBackground(requestDTO);
  }

  /** 이전 합성 결과 기록 (최신순) — 레거시 DB용. 신규 합성은 프론트 세션만 사용 */
  @PreAuthorize("hasAnyRole('USER')")
  @GetMapping("/history")
  public List<TryOnHistoryDTO> history(Principal principal) {
    return aiDressTryOnService.getHistory(principal.getName());
  }

  @PreAuthorize("hasAnyRole('USER')")
  @PutMapping("/history/{historyId}")
  public TryOnHistoryDTO updateHistory(
      Principal principal,
      @PathVariable Long historyId,
      @RequestBody TryOnHistoryUpdateDTO updateDTO) {
    return aiDressTryOnService.updateHistory(principal.getName(), historyId, updateDTO);
  }

  @PreAuthorize("hasAnyRole('USER')")
  @DeleteMapping("/history/{historyId}")
  public Map<String, String> deleteHistory(
      Principal principal,
      @PathVariable Long historyId) {
    aiDressTryOnService.deleteHistory(principal.getName(), historyId);
    return Map.of("result", "SUCCESS");
  }
}
