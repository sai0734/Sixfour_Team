package com.wedding.member.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

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

        Pageable pageable = PageRequest.of(
                searchDTO.getPage() - 1,
                searchDTO.getSize(),
                Sort.by("regDate").descending());

        String keyword = searchDTO.getKeyword();
        String status = searchDTO.getStatus();

        Page<Member> result = memberRepository.searchMembers(keyword, status, pageable);

        List<AdminMemberDTO> dtoList = result.get().map(this::toDTO).toList();

        return PageResponseDTO.<AdminMemberDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(searchDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    public void changeStatus(String email, MemberStatusUpdateDTO updateDTO) {

        log.info("AdminMemberServiceImpl_changeStatus 실행~~~~~~~~ " + email + " -> " + updateDTO);

        String status = updateDTO.getStatus();

        if (!VALID_STATUS.contains(status)) {
            throw new IllegalStateException("status는 ACTIVE, BLACKLIST, DORMANT 중 하나여야 합니다.");
        }

        Member member = memberRepository.findById(email)
                .orElseThrow(() -> new NoSuchElementException("해당 회원을 찾을 수 없습니다: " + email));

        if (member.getMemberRoleList().contains(MemberRole.ADMIN)) {
            throw new IllegalStateException("관리자 계정은 상태를 변경할 수 없습니다.");
        }

        switch (status) {
            case "BLACKLIST" -> {
                if (updateDTO.getReason() == null || updateDTO.getReason().isBlank()) {
                    throw new IllegalStateException("정지 처리에는 사유가 필요합니다.");
                }

                Integer days = updateDTO.getSuspendDays();
                LocalDateTime until = (days == null || days <= 0) ? null : LocalDateTime.now().plusDays(days);

                member.suspend(updateDTO.getReason(), until);
            }
            case "DORMANT" -> member.markDormant();
            case "ACTIVE" -> member.reactivate();
            default -> throw new IllegalStateException("지원하지 않는 상태값입니다: " + status);
        }

        memberRepository.save(member);
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