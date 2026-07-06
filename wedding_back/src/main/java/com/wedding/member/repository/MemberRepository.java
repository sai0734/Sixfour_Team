package com.wedding.member.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.wedding.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, String> {

  @EntityGraph(attributePaths = {"memberRoleList"})
  @Query("select m from Member m where m.email = :email")
  Member getWithRoles(@Param("email") String email);

  boolean existsByNickname(String nickname);


  @EntityGraph(attributePaths = {"memberRoleList"})
  @Query("select m from Member m " +
          "where (:keyword is null or :keyword = '' " +
          "       or m.nickname like concat('%', :keyword, '%') " +
          "       or m.email like concat('%', :keyword, '%')) " +
          "and (:status is null or :status = '' or m.status = :status)")
  Page<Member> searchMembers(@Param("keyword") String keyword,
                             @Param("status") String status,
                             Pageable pageable);


  @Query("select m from Member m where m.status = 'BLACKLIST' " +
          "and m.suspendUntil is not null and m.suspendUntil <= :now")
  List<Member> findExpiredSuspensions(@Param("now") LocalDateTime now);

}