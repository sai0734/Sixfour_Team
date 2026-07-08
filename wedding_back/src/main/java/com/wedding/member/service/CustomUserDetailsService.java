    package com.wedding.member.service;

    import java.time.LocalDateTime;
    import java.util.Optional;
    import java.util.stream.Collectors;

    import org.springframework.security.authentication.DisabledException;
    import org.springframework.security.authentication.LockedException;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.stereotype.Service;
    import com.wedding.member.domain.LoginFail;
    import com.wedding.member.domain.Member;
    import com.wedding.member.dto.MemberDTO;
    import com.wedding.member.repository.LoginFailRepository;
    import com.wedding.member.repository.MemberRepository;

    import lombok.RequiredArgsConstructor;
    import lombok.extern.log4j.Log4j2;

    @Service
    @Log4j2
    @RequiredArgsConstructor
    public class CustomUserDetailsService implements UserDetailsService{

        private final MemberRepository memberRepository;
        private final LoginFailRepository loginFailRepository;

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

            log.info("----------------loadUserByUsername-----------------------------");

            Member member = memberRepository.getWithRoles(username);

            if(member == null){
                throw new UsernameNotFoundException("Not Found");
            }


            Optional<LoginFail> loginFail = loginFailRepository.getByMemberEmail(username);

            if (loginFail.isPresent() && loginFail.get().isLocked()) {
                throw new LockedException("로그인 5회 실패로 계정이 잠겼습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요.");
            }


            if ("BLACKLIST".equals(member.getStatus())
                    && member.getSuspendUntil() != null
                    && !member.getSuspendUntil().isAfter(LocalDateTime.now())) {
                member.reactivate();
                memberRepository.save(member);
            }

            if ("BLACKLIST".equals(member.getStatus())) {
                throw new DisabledException("정지된 계정입니다. 사유: " + member.getSuspendReason());
            }

            if ("DORMANT".equals(member.getStatus())) {
                throw new DisabledException("장기 미접속으로 휴면 처리된 계정입니다. 계정 재활성화가 필요합니다.");
            }

            MemberDTO memberDTO = new MemberDTO(
                    member.getEmail(),
                    member.getPw(),
                    member.getNickname(),
                    member.isSocial(),
                    member.getMemberRoleList()
                            .stream()
                            .map(memberRole -> memberRole.name()).collect(Collectors.toList()));

            log.info(memberDTO);

            return memberDTO;


        }

    }