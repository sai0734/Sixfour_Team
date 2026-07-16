package com.wedding.aidress.controller;

import com.wedding.aidress.dto.AiDressTryOnRequestDTO;
import com.wedding.aidress.dto.AiDressTryOnResponseDTO;
import com.wedding.aidress.dto.DressTryOnItemDTO;
import com.wedding.aidress.service.AiDressTryOnService;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  // 드레스 목록 (DB)
  @GetMapping("/dresses")
  public PageResponseDTO<DressTryOnItemDTO> dresses(PageRequestDTO pageRequestDTO) {
    return aiDressTryOnService.getDressList(pageRequestDTO);
  }

  // 내 사진 저장
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping(value = "/my-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, String> uploadMyPhoto(
      Principal principal,
      @RequestPart("file") MultipartFile file) {
    String fileName = aiDressTryOnService.saveMyPhoto(principal.getName(), file);
    return Map.of("photoFileName", fileName);
  }

  // 내 사진 조회
  @PreAuthorize("hasAnyRole('USER')")
  @GetMapping("/my-photo")
  public Map<String, String> getMyPhoto(Principal principal) {
    return Map.of("photoFileName", aiDressTryOnService.getMyPhoto(principal.getName()));
  }

  // 합성 실행
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/try-on")
  public AiDressTryOnResponseDTO tryOn(
      Principal principal,
      @RequestBody AiDressTryOnRequestDTO requestDTO) {
    return aiDressTryOnService.tryOn(principal.getName(), requestDTO);
  }
}
