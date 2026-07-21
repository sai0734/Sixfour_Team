package com.wedding.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;

public interface MemberRepository extends JpaRepository<Member, String> {

  // JOIN FETCH를 사용하여 권한 리스트를 즉시 로딩하도록 강제
  @Query("select m from Member m left join fetch m.memberRoleList where m.email = :email")
  Member getWithRoles(@Param("email") String email);

  boolean existsByNickname(String nickname);

  // 관리자 회원관리 "회원 목록" 탭 - 일반 사용자만 노출 (업체 담당자/관리자로 권한이 바뀌면 이 목록에서 빠짐)
  @EntityGraph(attributePaths = {"memberRoleList"})
  @Query("select m from Member m " +
          "where (:keyword is null or :keyword = '' " +
          "       or m.nickname like concat('%', :keyword, '%') " +
          "       or m.email like concat('%', :keyword, '%')) " +
          "and (:status is null or :status = '' or m.status = :status) " +
          "and com.wedding.member.domain.MemberRole.ADMIN not member of m.memberRoleList " +
          "and com.wedding.member.domain.MemberRole.MANAGER not member of m.memberRoleList")
  Page<Member> searchMembers(@Param("keyword") String keyword,
                             @Param("status") String status,
                             Pageable pageable);

  @Query("select m from Member m where m.status = 'BLACKLIST' " +
          "and m.suspendUntil is not null and m.suspendUntil <= :now")
  List<Member> findExpiredSuspensions(@Param("now") LocalDateTime now);

  // 자동 휴면 전환용 - 정상(ACTIVE) 회원 중 마지막 로그인(없으면 가입일 기준)이 cutoff 이전인 회원
  @Query("select m from Member m where m.status = 'ACTIVE' " +
          "and coalesce(m.lastLoginAt, m.regDate) <= :cutoff")
  List<Member> findInactiveForAutoDormant(@Param("cutoff") LocalDateTime cutoff);

  // 회원 관리 "관리자 목록" 탭 - 관리자 권한을 가진 회원 전체
  @EntityGraph(attributePaths = {"memberRoleList"})
  @Query("select m from Member m where :role member of m.memberRoleList order by m.email")
  List<Member> findByRoleContaining(@Param("role") MemberRole role);

  // 관리자 대시보드용 집계
  long countByStatus(String status);

  // 관리자 대시보드 "전체 회원" - 관리자/업체담당자를 뺀 일반 사용자 중 정상(ACTIVE) 회원 수
  @Query("select count(m) from Member m " +
          "where m.status = 'ACTIVE' " +
          "and com.wedding.member.domain.MemberRole.ADMIN not member of m.memberRoleList " +
          "and com.wedding.member.domain.MemberRole.MANAGER not member of m.memberRoleList")
  long countActiveRegularMembers();

  // 관리자 대시보드 "오늘 신규" - 가입 후 바로 탈퇴한 회원은 신규 가입자 수에서 빠짐
  long countByRegDateAfterAndStatusNot(LocalDateTime dateTime, String status);

  long countByEmailVerifiedFalse();
}