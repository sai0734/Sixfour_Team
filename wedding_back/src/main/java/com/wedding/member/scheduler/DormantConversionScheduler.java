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

// 마지막 로그인(가입 후 한 번도 로그인 안 했으면 가입일)로부터 1년이 지난 ACTIVE 회원을
// 매일 자정에 자동으로 휴면(DORMANT) 처리. 관리자가 수동으로 "휴면 전환"시키던 것을 대체함.
// 휴면 처리된 계정은 CustomUserDetailsService에서 로그인 자체를 막고 별도 재활성화가 필요함.
@Component
@Log4j2
@RequiredArgsConstructor
public class DormantConversionScheduler {

    private static final long INACTIVITY_YEARS = 1;

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void convertInactiveMembersToDormant() {

        LocalDateTime cutoff = LocalDateTime.now().minusYears(INACTIVITY_YEARS);
        List<Member> inactive = memberRepository.findInactiveForAutoDormant(cutoff);

        if (inactive.isEmpty()) {
            return;
        }

        log.info("1년 이상 미접속으로 자동 휴면 전환되는 회원 수: " + inactive.size());

        inactive.forEach(Member::markDormant);

        memberRepository.saveAll(inactive);
    }

}
