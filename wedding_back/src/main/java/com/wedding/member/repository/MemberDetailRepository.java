package com.wedding.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.member.domain.MemberDetail;

public interface MemberDetailRepository extends JpaRepository<MemberDetail, Long> {

  @Query("select md from MemberDetail md where md.member.email = :email")
  Optional<MemberDetail> getByMemberEmail(@Param("email") String email);

  List<MemberDetail> findAllByPhone(String phone);

}