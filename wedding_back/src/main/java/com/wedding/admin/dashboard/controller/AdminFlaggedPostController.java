package com.wedding.admin.dashboard.controller;

import com.wedding.admin.dashboard.domain.FlaggedPost;
import com.wedding.admin.dashboard.repository.FlaggedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/flagged-posts")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminFlaggedPostController {

    private final FlaggedPostRepository flaggedPostRepository;

    // 최신 5건만 반환 (무한정 쌓여도 응답이 커지지 않도록) - 전체 건수는 /count로 별도 확인
    @GetMapping
    public List<FlaggedPost> list() {
        return flaggedPostRepository.findTop5ByResolvedFalseOrderByRegDateDesc();
    }

    @GetMapping("/count")
    public long count() {
        return flaggedPostRepository.countByResolvedFalse();
    }

    @PutMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        flaggedPostRepository.findById(id).ifPresent(post -> {
            post.resolve();
            flaggedPostRepository.save(post);
        });
    }
}
