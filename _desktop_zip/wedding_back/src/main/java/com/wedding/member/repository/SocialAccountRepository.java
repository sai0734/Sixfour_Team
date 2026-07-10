package com.wedding.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.member.domain.SocialAccount;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

  Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

  List<SocialAccount> findAllByMember_Email(String email);

  Optional<SocialAccount> findByMember_EmailAndProvider(String email, String provider);

  void deleteAllByMember_Email(String email);

}