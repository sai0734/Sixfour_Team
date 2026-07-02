package com.wedding.member.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.wedding.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, String> {

  @EntityGraph(attributePaths = {"memberRoleList"})
  @Query("select m from Member m where m.email = :email")
  Member getWithRoles(@Param("email") String email);

  boolean existsByNickname(String nickname);

}