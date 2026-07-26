package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.domain.AiBriefing;
import com.wedding.admin.dashboard.domain.FlaggedPost;
import com.wedding.admin.dashboard.domain.SiteHealthIssue;
import com.wedding.admin.dashboard.repository.AiBriefingRepository;
import com.wedding.admin.dashboard.repository.FlaggedPostRepository;
import com.wedding.admin.dashboard.repository.SiteHealthIssueRepository;
import com.wedding.board.domain.Board;
import com.wedding.board.repository.BoardRepository;
import com.wedding.global.util.CustomFileUtil;
import com.wedding.product.repository.ProductRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

// OpenClaw(로컬에서 도는 개인 자동화 에이전트)가 우리 백엔드에 결과를 보고하는 전용 API.
// 로그인 세션이 없는 외부 프로세스라 JWT 대신 고정 시크릿 헤더(X-OpenClaw-Key)로만 인증한다.
// JWTCheckFilter.shouldNotFilter()에서 "/api/openclaw/"는 JWT 검사를 건너뛰도록 예외 처리되어 있음.
@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/openclaw")
public class OpenClawIngestController {

    private static final Set<String> VALID_FLAG_TYPES = Set.of("SPAM", "PERSONAL_ATTACK", "PROFANITY", "OTHER");

    private final SiteHealthIssueRepository siteHealthIssueRepository;
    private final FlaggedPostRepository flaggedPostRepository;
    private final AiBriefingRepository aiBriefingRepository;
    private final BoardRepository boardRepository;
    private final ProductRepository productRepository;
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

        // IMAGE_BROKEN이고 productPno가 왔으면, AI가 쓴 문장을 믿지 않고 서버가 DB에서
        // 상품명을 직접 읽어와 고정된 문구로 조합한다 - AI가 콘솔 인코딩 문제로 상품명을
        // 깨뜨려 보내는 문제를 원천 차단하고, 카드 문구 형식도 항상 동일하게 유지된다.
        String detail = dto.getDetail();
        if ("IMAGE_BROKEN".equals(dto.getIssueType()) && dto.getProductPno() != null) {
            detail = productRepository.findById(dto.getProductPno())
                    .map(p -> String.format("%s(상품번호 %d)의 대표 이미지를 불러올 수 없습니다.",
                            p.getPname(), dto.getProductPno()))
                    .orElseGet(() -> String.format("상품번호 %d의 대표 이미지를 불러올 수 없습니다. (상품 정보를 찾을 수 없음)",
                            dto.getProductPno()));
        }

        SiteHealthIssue issue = SiteHealthIssue.builder()
                .pageUrl(dto.getPageUrl())
                .issueType(dto.getIssueType())
                .productPno(dto.getProductPno())
                .detail(detail)
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

        // 게시글 제목은 AI가 쓴 텍스트가 아니라 서버가 board 테이블에서 직접 조회해 스냅샷으로
        // 저장한다 - 인코딩이 깨지지 않고, 나중에 게시글이 삭제돼도 제목이 카드에 남아있다.
        String boardTitle = boardRepository.findById(dto.getBoardId())
                .map(Board::getTitle)
                .orElse(null);

        String flagType = VALID_FLAG_TYPES.contains(dto.getFlagType()) ? dto.getFlagType() : "OTHER";

        FlaggedPost flaggedPost = FlaggedPost.builder()
                .boardId(dto.getBoardId())
                .flagType(flagType)
                .boardTitle(boardTitle)
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
        private Long productPno;
    }

    @Data
    public static class FlaggedPostRequestDTO {
        private Long boardId;
        private String flagType;
        private String reason;
    }
}
