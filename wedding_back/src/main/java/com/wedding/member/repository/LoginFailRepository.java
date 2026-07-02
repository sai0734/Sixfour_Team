package com.wedding.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.member.domain.LoginFail;

public interface LoginFailRepository extends JpaRepository<LoginFail, Long> {

  @Query("select lf from LoginFail lf where lf.member.email = :email")
  Optional<LoginFail> getByMemberEmail(@Param("email") String email);

}