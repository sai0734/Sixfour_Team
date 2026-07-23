package com.wedding.quote.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.quote.domain.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    // 마이 견적서 목록 - 최신 업로드 순
    List<Quote> findByMemberEmailOrderByQuoteIdDesc(String memberEmail);

    // 정리 배치(QuoteCleanupScheduler)용 - 업로드 후 TTL 지난 것들
    List<Quote> findByRegDateBefore(LocalDateTime cutoff);
}
