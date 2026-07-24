package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.domain.AiBriefing;
import com.wedding.admin.dashboard.repository.AiBriefingRepository;
import com.wedding.global.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai-briefing")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminAiBriefingController {

    private final AiBriefingRepository aiBriefingRepository;
    private final CustomFileUtil customFileUtil;

    // 브리핑 목록 조회 (최신순)
    @GetMapping
    public List<AiBriefing> list() {
        return aiBriefingRepository.findAllByOrderByRegDateDesc();
    }

    // 브리핑 PDF 파일 조회
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable Long id) {
        AiBriefing briefing = aiBriefingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return customFileUtil.getFile(briefing.getPdfFileName());
    }

    // 브리핑 삭제 (PDF 파일도 함께 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        AiBriefing briefing = aiBriefingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        customFileUtil.deleteFiles(List.of(briefing.getPdfFileName()));
        aiBriefingRepository.delete(briefing);

        return ResponseEntity.ok().build();
    }
}
