package com.wedding.aidress.controller;

import com.wedding.aidress.dto.AiDressTryOnRequestDTO;
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

  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping(value = "/my-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, String> uploadMyPhoto(
      Principal principal,
      @RequestPart("file") MultipartFile file) {
    String fileName = aiDressTryOnService.saveMyPhoto(principal.getName(), file);
    return Map.of("photoFileName", fileName);
  }

  @PreAuthorize("hasAnyRole('USER')")
  @GetMapping("/my-photo")
  public Map<String, String> getMyPhoto(Principal principal) {
    return Map.of("photoFileName", aiDressTryOnService.getMyPhoto(principal.getName()));
  }

  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/try-on")
  public AiDressTryOnResponseDTO tryOn(
      Principal principal,
      @RequestBody AiDressTryOnRequestDTO requestDTO) {
    return aiDressTryOnService.tryOn(principal.getName(), requestDTO);
  }

  /** 이전 합성 결과 기록 (최신순) */
  @PreAuthorize("hasAnyRole('USER')")
  @GetMapping("/history")
  public List<TryOnHistoryDTO> history(Principal principal) {
    return aiDressTryOnService.getHistory(principal.getName());
  }

  /** 배경 프롬프트 수정(재합성). 프롬프트 비우면 CatVTON 원본으로 되돌림 */
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
