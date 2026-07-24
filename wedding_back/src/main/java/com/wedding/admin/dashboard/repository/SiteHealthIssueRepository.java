package com.wedding.admin.dashboard.repository;

import com.wedding.admin.dashboard.domain.SiteHealthIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteHealthIssueRepository extends JpaRepository<SiteHealthIssue, Long> {

    // 플로팅 패널은 최신 N건만 보여준다 - 방치돼서 무한정 쌓여도 응답 크기가 커지지 않도록
    List<SiteHealthIssue> findTop5ByResolvedFalseOrderByRegDateDesc();

    // 미해결 건수 조회
    long countByResolvedFalse();
}
