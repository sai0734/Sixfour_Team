package com.wedding.companywish.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.companywish.domain.CompanyWish;

public interface CompanyWishRepository extends JpaRepository<CompanyWish, Long> {

    /** 찜 여부 확인 (옵션 상관없이 이 업체를 하나라도 찜했는지) */
    boolean existsByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 이메일+cmno로 찜 단건 조회 */
    Optional<CompanyWish> findByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 이메일+cmno로 찜 삭제 (옵션 없는 업체 - 예: 스튜디오) */
    void deleteByMemberEmailAndCmno(String memberEmail, Long cmno);

    /** 마이페이지 찜 목록 조회 (최신순) */
    List<CompanyWish> findByMemberEmailOrderByWishIdDesc(String memberEmail);

    // ↓↓↓ 재원 추가 - 옵션(홀/드레스/메이크업)별로 찜 여러 건이 가능해지면서 필요해진 메서드들

    /** 특정 옵션으로 이미 찜했는지 확인 (중복 찜 방지) */
    boolean existsByMemberEmailAndCmnoAndOptionName(String memberEmail, Long cmno, String optionName);

    Optional<CompanyWish> findByMemberEmailAndCmnoAndOptionName(String memberEmail, Long cmno, String optionName);

    /** 특정 옵션의 찜 삭제 */
    void deleteByMemberEmailAndCmnoAndOptionName(String memberEmail, Long cmno, String optionName);

    /** 마이페이지에서 wishId로 정확히 찍어서 삭제 (본인 소유 확인 겸용) */
    void deleteByWishIdAndMemberEmail(Long wishId, String memberEmail);

    /** 업체 상세페이지 옵션 하트 표시용 - 이 업체에서 내가 찜한 항목 전체 (옵션별로 여러 건일 수 있음) */
    List<CompanyWish> findAllByMemberEmailAndCmno(String memberEmail, Long cmno);
    // ↑↑↑ 재원 추가

}