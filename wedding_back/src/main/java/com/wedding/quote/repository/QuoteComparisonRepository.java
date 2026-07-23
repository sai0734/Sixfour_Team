package com.wedding.quote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.quote.domain.QuoteComparison;

public interface QuoteComparisonRepository extends JpaRepository<QuoteComparison, Long> {

    // 비교 기록 목록 - 최신 비교부터
    List<QuoteComparison> findByMemberEmailOrderByComparisonIdDesc(String memberEmail);
}
