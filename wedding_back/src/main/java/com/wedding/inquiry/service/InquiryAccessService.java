package com.wedding.inquiry.service;

import com.wedding.company.domain.Company;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.inquiry.domain.InquiryRoom;
import com.wedding.inquiry.repository.InquiryRoomRepository;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 문의 API 권한 검증을 한곳에서 처리한다.
 * Controller의 @PreAuthorize와 Service 내부 분기를 대신한다.
 */
@Service
@RequiredArgsConstructor
public class InquiryAccessService {

    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final InquiryRoomRepository inquiryRoomRepository;

    public void requireCanOpenRoom(String callerEmail, Long cmno) {
        Member member = requireMember(callerEmail);
        requireCompanyExists(cmno);

        if (hasRole(member, MemberRole.MANAGER) && !hasRole(member, MemberRole.ADMIN)) {
            throw new AccessDeniedException("매니저는 문의방을 생성할 수 없습니다.");
        }
        if (!hasRole(member, MemberRole.USER) && !hasRole(member, MemberRole.ADMIN)) {
            throw new AccessDeniedException("문의 권한이 없습니다.");
        }
        if (isManagerOfCompany(callerEmail, cmno)) {
            throw new AccessDeniedException("담당 업체에는 문의할 수 없습니다.");
        }
    }

    public void requireCanListCompanyRooms(String callerEmail, Long cmno) {
        requireMember(callerEmail);
        requireCompanyExists(cmno);

        if (isAdmin(callerEmail) || isManagerOfCompany(callerEmail, cmno)) {
            return;
        }
        throw new AccessDeniedException("해당 업체 문의함에 접근할 권한이 없습니다.");
    }

    public InquiryRoom requireAccessibleRoom(String callerEmail, Long roomId) {
        InquiryRoom room = inquiryRoomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("문의방을 찾을 수 없습니다."));
        requireCanAccessRoom(callerEmail, room);
        return room;
    }

    public void requireCanAccessRoom(String callerEmail, InquiryRoom room) {
        requireMember(callerEmail);

        if (isAdmin(callerEmail)) {
            return;
        }
        if (room.getMemberEmail().equals(callerEmail)) {
            return;
        }
        if (isManagerOfCompany(callerEmail, room.getCmno())) {
            return;
        }
        throw new AccessDeniedException("해당 문의방에 접근할 권한이 없습니다.");
    }

    private void requireCompanyExists(Long cmno) {
        if (!companyRepository.existsById(cmno)) {
            throw new NoSuchElementException("업체를 찾을 수 없습니다.");
        }
    }

    private Member requireMember(String email) {
        Member member = memberRepository.getWithRoles(email);
        if (member == null) {
            throw new AccessDeniedException("회원 정보를 확인할 수 없습니다.");
        }
        return member;
    }

    private boolean isAdmin(String email) {
        Member member = memberRepository.getWithRoles(email);
        return member != null && hasRole(member, MemberRole.ADMIN);
    }

    private boolean isManagerOfCompany(String email, Long cmno) {
        return companyRepository.findByManagerEmail(email)
                .map(Company::getCmno)
                .filter(managedCmno -> managedCmno.equals(cmno))
                .isPresent();
    }

    private boolean hasRole(Member member, MemberRole role) {
        return member.getMemberRoleList().contains(role);
    }
}
