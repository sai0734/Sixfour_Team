package com.wedding.member.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.member.domain.Member;
import com.wedding.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class SuspensionExpiryScheduler {

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void reactivateExpiredSuspensions() {

        List<Member> expired = memberRepository.findExpiredSuspensions(LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("정지 기간 만료로 자동 해제되는 회원 수: " + expired.size());

        expired.forEach(Member::reactivate);

        memberRepository.saveAll(expired);
    }

}