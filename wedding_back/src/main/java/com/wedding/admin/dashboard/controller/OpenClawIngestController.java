package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.domain.AiBriefing;
import com.wedding.admin.dashboard.domain.FlaggedPost;
import com.wedding.admin.dashboard.domain.SiteHealthIssue;
import com.wedding.admin.dashboard.repository.AiBriefingRepository;
import com.wedding.admin.dashboard.repository.FlaggedPostRepository;
import com.wedding.admin.dashboard.repository.SiteHealthIssueRepository;
import com.wedding.global.util.CustomFileUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// OpenClaw(로컬에서 도는 개인 자동화 에이전트)가 우리 백엔드에 결과를 보고하는 전용 API.
// 로그인 세션이 없는 외부 프로세스라 JWT 대신 고정 시크릿 헤더(X-OpenClaw-Key)로만 인증한다.
// JWTCheckFilter.shouldNotFilter()에서 "/api/openclaw/"는 JWT 검사를 건너뛰도록 예외 처리되어 있음.
@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/openclaw")
public class OpenClawIngestController {

    private final SiteHealthIssueRepository siteHealthIssueRepository;
    private final FlaggedPostRepository flaggedPostRepository;
    private final AiBriefingRepository aiBriefingRepository;
    private final CustomFileUtil customFileUtil;

    @Value("${openclaw.internal-key:}")
    private String openClawInternalKey;

    @PostMapping("/site-health-issues")
    public ResponseEntity<?> reportSiteHealthIssue(
            @RequestHeader("X-OpenClaw-Key") String key,
            @RequestBody SiteHealthIssueRequestDTO dto) {

        if (!isKeyValid(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SiteHealthIssue issue = SiteHealthIssue.builder()
                .pageUrl(dto.getPageUrl())
                .issueType(dto.getIssueType())
                .detail(dto.getDetail())
                .build();

        siteHealthIssueRepository.save(issue);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/flagged-posts")
    public ResponseEntity<?> reportFlaggedPost(
            @RequestHeader("X-OpenClaw-Key") String key,
            @RequestBody FlaggedPostRequestDTO dto) {

        if (!isKeyValid(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FlaggedPost flaggedPost = FlaggedPost.builder()
                .boardId(dto.getBoardId())
                .reason(dto.getReason())
                .build();

        flaggedPostRepository.save(flaggedPost);

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/ai-briefing", consumes = "multipart/form-data")
    public ResponseEntity<?> reportAiBriefing(
            @RequestHeader("X-OpenClaw-Key") String key,
            @RequestParam("file") MultipartFile file,
            @RequestParam("weekOf") String weekOf,
            @RequestParam("summaryText") String summaryText,
            @RequestParam(value = "lowStockCount", defaultValue = "0") int lowStockCount,
            @RequestParam(value = "flaggedPostCount", defaultValue = "0") int flaggedPostCount,
            @RequestParam(value = "siteIssueCount", defaultValue = "0") int siteIssueCount) {

        if (!isKeyValid(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> savedNames = customFileUtil.saveFiles(List.of(file));
        String pdfFileName = savedNames.get(0);

        AiBriefing briefing = AiBriefing.builder()
                .weekOf(weekOf)
                .summaryText(summaryText)
                .pdfFileName(pdfFileName)
                .lowStockCount(lowStockCount)
                .flaggedPostCount(flaggedPostCount)
                .siteIssueCount(siteIssueCount)
                .build();

        aiBriefingRepository.save(briefing);

        return ResponseEntity.ok().build();
    }

    private boolean isKeyValid(String key) {
        if (openClawInternalKey.isBlank() || !openClawInternalKey.equals(key)) {
            log.warn("OpenClaw 요청 키 불일치 - 접근 거부");
            return false;
        }
        return true;
    }

    @Data
    public static class SiteHealthIssueRequestDTO {
        private String pageUrl;
        private String issueType;
        private String detail;
    }

    @Data
    public static class FlaggedPostRequestDTO {
        private Long boardId;
        private String reason;
    }
}
