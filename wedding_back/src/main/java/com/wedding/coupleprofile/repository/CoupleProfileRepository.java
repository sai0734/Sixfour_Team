package com.wedding.coupleprofile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.coupleprofile.domain.CoupleProfile;

public interface CoupleProfileRepository extends JpaRepository<CoupleProfile, Long> {

    Optional<CoupleProfile> findByMemberEmail(String memberEmail);

    // 매칭 화면에서 보여줄 전체 목록 (본인 제외는 서비스에서 처리)
    List<CoupleProfile> findAllByOrderByProfileIdDesc();

}
