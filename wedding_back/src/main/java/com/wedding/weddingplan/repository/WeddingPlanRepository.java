package com.wedding.weddingplan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.weddingplan.domain.WeddingPlan;

public interface WeddingPlanRepository extends JpaRepository<WeddingPlan, Long> {

    // Member와 1:1 관계라 목록이 아닌 단건 조회
    Optional<WeddingPlan> findByMemberEmail(String memberEmail);

}
