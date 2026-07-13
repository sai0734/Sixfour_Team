package com.wedding.companywish.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.companywish.domain.CompanyWish;

public interface CompanyWishRepository extends JpaRepository<CompanyWish, Long> {

    /** 찜 여부 확인 */
    boolean existsByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 이메일+cmno로 찜 단건 조회 */
    Optional<CompanyWish> findByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 이메일+cmno로 찜 삭제 */
    void deleteByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 마이페이지 찜 목록 조회 (최신순) */
    List<CompanyWish> findByMemberEmailOrderByWishIdDesc(String memberEmail);
}
