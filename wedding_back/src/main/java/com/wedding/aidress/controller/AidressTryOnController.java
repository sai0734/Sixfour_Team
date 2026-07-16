package com.wedding.aidress.controller;

import com.wedding.company.domain.DressTryOnItemDTO;
import com.wedding.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-dress")
public class AidressTryOnController {

    private final AidressTryOnController aidressTryOnController;

    //드레스목록(DB)
    @GetMapping("/dresses")
    public PageResponseDTO<DressTryOnItemDTO> dresses(pageRequestDTO pageRequestDTO) {
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
    @PreAuthorize("hasAnyRole(`user`)")
    @GetMapping("/my-photo")
    public Map<String, String> getMyPhoto(Principal principal) {
        return Map.of("phototoFileName", aiDressTryonService.getMyPhoto(principal.getName()));
    }

    //합성 실행
    @PreAuthorize("hasAnyRole(`user`)")
    @PostMapping("/try-on")
    public AiDressTryOnResponseDTO tryOn(
            Principal principal,
            @RequestBody AiDressTryOnRequestDTO requestDTO) {
        return aidressTryOnService.tryOn(principal.getName(), requestDTO);
    }
}
