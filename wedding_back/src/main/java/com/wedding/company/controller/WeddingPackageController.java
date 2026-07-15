package com.wedding.company.controller;

import com.wedding.company.dto.WeddingPackageDTO;
import com.wedding.company.dto.WeddingPackageSearchDTO;
import com.wedding.company.service.WeddingPackageService;
import com.wedding.global.dto.PageResponseDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/packages")
public class WeddingPackageController {

    private final WeddingPackageService weddingPackageService;

    @GetMapping("/list")
    public PageResponseDTO<com.wedding.company.dto.WeddingPackageListDTO> list(
            WeddingPackageSearchDTO searchDTO) {
        return weddingPackageService.getList(searchDTO);
    }

    @GetMapping("/{weddingPackageId}")
    public WeddingPackageDTO get(@PathVariable Long weddingPackageId) {
        return weddingPackageService.get(weddingPackageId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody WeddingPackageDTO dto) {
        Long id = weddingPackageService.register(dto);
        return Map.of("weddingPackageId", id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{weddingPackageId}")
    public Map<String, String> modify(@PathVariable Long weddingPackageId,
                                      @RequestBody WeddingPackageDTO dto) {
        dto.setWeddingPackageId(weddingPackageId);
        weddingPackageService.modify(dto);
        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{weddingPackageId}")
    public Map<String, String> remove(@PathVariable Long weddingPackageId) {
        weddingPackageService.remove(weddingPackageId);
        return Map.of("RESULT", "SUCCESS");
    }
}