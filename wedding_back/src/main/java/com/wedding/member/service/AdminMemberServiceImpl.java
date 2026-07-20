package com.wedding.member.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.dto.AdminMemberDTO;
import com.wedding.member.dto.AdminMemberSearchDTO;
import com.wedding.member.dto.MemberStatusUpdateDTO;
import com.wedding.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class AdminMemberServiceImpl implements AdminMemberService {

    private static final List<String> VALID_STATUS = List.of("ACTIVE", "BLACKLIST", "DORMANT");
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminMemberDTO> getMemberList(AdminMemberSearchDTO searchDTO) {
        Pageable pageable = PageRequest.of(searchDTO.getPage() - 1, searchDTO.getSize(), Sort.by("regDate").descending());
        Page<Member> result = memberRepository.searchMembers(searchDTO.getKeyword(), searchDTO.getStatus(), pageable);
        List<AdminMemberDTO> dtoList = result.get().map(this::toDTO).toList();
        return PageResponseDTO.<AdminMemberDTO>withAll().dtoList(dtoList).pageRequestDTO(searchDTO).totalCount(result.getTotalElements()).build();
    }

    @Override
    public void changeStatus(String email, MemberStatusUpdateDTO updateDTO) {
        Member member = memberRepository.findById(email).orElseThrow();
        if (member.getMemberRoleList().contains(MemberRole.ADMIN)) throw new IllegalStateException("관리자 계정은 변경 불가");

        switch (updateDTO.getStatus()) {
            case "BLACKLIST" -> member.suspend(updateDTO.getReason(), LocalDateTime.now().plusDays(updateDTO.getSuspendDays()));
            case "DORMANT" -> member.markDormant();
            case "ACTIVE" -> member.reactivate();
        }
        memberRepository.save(member);
    }

    @Override
    public void changeRole(String email, String role, String requesterEmail) {
        if (email.equals(requesterEmail)) {
            throw new IllegalStateException("본인의 권한은 변경할 수 없습니다.");
        }

        Member member = memberRepository.findById(email).orElseThrow();

        if ("ADMIN".equals(role)) {
            member.addRole(MemberRole.ADMIN);
        } else if ("USER".equals(role)) {
            member.removeRole(MemberRole.ADMIN);
        } else {
            throw new IllegalArgumentException("알 수 없는 권한: " + role);
        }

        memberRepository.save(member);
    }


    @Override
    @Transactional(readOnly = true)
    public List<AdminMemberDTO> getAdminList() {
        return memberRepository.findByRoleContaining(MemberRole.ADMIN).stream()
                .map(this::toDTO)
                .toList();
    }

    private AdminMemberDTO toDTO(Member member) {
        return AdminMemberDTO.builder()
                .email(member.getEmail())
                .nickname(member.getNickname())
                .regDate(member.getRegDate())
                .lastLoginAt(member.getLastLoginAt())
                .status(member.getStatus())
                .suspendReason(member.getSuspendReason())
                .suspendUntil(member.getSuspendUntil())
                .emailVerified(member.isEmailVerified())
                .admin(member.getMemberRoleList().contains(MemberRole.ADMIN))
                .build();
    }
}