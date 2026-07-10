package com.wedding.faq.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.faq.domain.FaqLike;

public interface FaqLikeRepository extends JpaRepository<FaqLike, Long> {

    boolean existsByFaqIdAndMemberEmail(Long faqId, String memberEmail);

    Optional<FaqLike> findByFaqIdAndMemberEmail(Long faqId, String memberEmail);

}
