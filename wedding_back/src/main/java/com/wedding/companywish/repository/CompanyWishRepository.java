package com.wedding.companywish.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.companywish.domain.CompanyWish;

public interface CompanyWishRepository extends JpaRepository<CompanyWish, Long> {

    List<CompanyWish> findByMemberEmail(String memberEmail);

    // 중복 찜 여부 확인 (등록 전 체크용)
    boolean existsByMemberEmailAndCmno(String memberEmail, Long cmno);

    // 찜 취소 시 wishId를 모르고 memberEmail+cmno로만 아는 경우 대비
    Optional<CompanyWish> findByMemberEmailAndCmno(String memberEmail, Long cmno);

}
