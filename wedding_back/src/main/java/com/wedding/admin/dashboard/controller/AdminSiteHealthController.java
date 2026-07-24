package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.domain.SiteHealthIssue;
import com.wedding.admin.dashboard.repository.SiteHealthIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/site-health")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminSiteHealthController {

    private final SiteHealthIssueRepository siteHealthIssueRepository;

    // 최신 5건만 반환 (무한정 쌓여도 응답이 커지지 않도록) - 전체 건수는 /count로 별도 확인
    @GetMapping
    public List<SiteHealthIssue> list() {
        return siteHealthIssueRepository.findTop5ByResolvedFalseOrderByRegDateDesc();
    }

    // 미해결 건수 조회
    @GetMapping("/count")
    public long count() {
        return siteHealthIssueRepository.countByResolvedFalse();
    }

    // 처리 완료 표시
    @PutMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        siteHealthIssueRepository.findById(id).ifPresent(issue -> {
            issue.resolve();
            siteHealthIssueRepository.save(issue);
        });
    }
}
