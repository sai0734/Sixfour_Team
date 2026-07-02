package com.wedding.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.member.domain.EmailVerify;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {

  Optional<EmailVerify> findByToken(String token);

  Optional<EmailVerify> findTopByEmailOrderByIdDesc(String email);

}