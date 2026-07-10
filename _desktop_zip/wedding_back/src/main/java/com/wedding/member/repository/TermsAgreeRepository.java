package com.wedding.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.member.domain.TermsAgree;

public interface TermsAgreeRepository extends JpaRepository<TermsAgree, Long> {

  @Query("select ta from TermsAgree ta where ta.member.email = :email order by ta.regDate desc")
  List<TermsAgree> getListByMemberEmail(@Param("email") String email);

}