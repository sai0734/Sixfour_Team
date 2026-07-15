package com.wedding.reservation.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.wedding.company.domain.Company;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationAccessService {

  private final MemberRepository memberRepository;
  private final CompanyRepository companyRepository;

  public void requireCanManageCompanyReservations(String callerEmail, Long cmno) {
    requireMember(callerEmail);
    requireCompanyExists(cmno);

    if (isAdmin(callerEmail) || isManagerOfCompany(callerEmail, cmno)) {
      return;
    }

    throw new AccessDeniedException("해당 업체 예약관리에 접근할 권한이 없습니다.");
  }

  private void requireCompanyExists(Long cmno) {
    if (!companyRepository.existsById(cmno)) {
      throw new IllegalArgumentException("업체를 찾을 수 없습니다.");
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
