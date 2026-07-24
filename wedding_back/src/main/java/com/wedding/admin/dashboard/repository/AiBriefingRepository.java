package com.wedding.admin.dashboard.repository;

import com.wedding.admin.dashboard.domain.AiBriefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiBriefingRepository extends JpaRepository<AiBriefing, Long> {

    // 브리핑 목록 조회 (최신순)
    List<AiBriefing> findAllByOrderByRegDateDesc();
}
