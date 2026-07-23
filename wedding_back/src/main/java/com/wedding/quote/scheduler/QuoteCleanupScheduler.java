package com.wedding.quote.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.global.util.CustomFileUtil;
import com.wedding.quote.domain.Quote;
import com.wedding.quote.repository.QuoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AI 견적서는 별도 "저장/즐겨찾기" 개념 없이, 업로드 후 일정 기간이 지나면 예외 없이 전부
// 삭제한다(AI웨딩플랜 세션 정리(AiPlanSessionCleanupScheduler)와 달리 "적용 여부" 플래그가
// 없는 단순 flat TTL) - 견적서 가격 정보를 계속 서버에 쌓아두지 않기 위함.
@Component
@Log4j2
@RequiredArgsConstructor
public class QuoteCleanupScheduler {

    private final QuoteRepository quoteRepository;
    private final CustomFileUtil customFileUtil;

    @Value("${com.wedding.quote.retention-days}")
    private int retentionDays;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredQuotes() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Quote> expired = quoteRepository.findByRegDateBefore(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("업로드 후 " + retentionDays + "일 지나 자동 삭제되는 AI 견적서 수: " + expired.size());

        customFileUtil.deleteFiles(expired.stream().map(Quote::getImageFileName).toList());
        quoteRepository.deleteAll(expired);
    }

}
