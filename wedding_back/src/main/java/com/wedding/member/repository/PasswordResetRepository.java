package com.wedding.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.member.domain.PasswordReset;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

  Optional<PasswordReset> findByToken(String token);

}