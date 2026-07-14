package com.wedding.companywish.service;

import java.util.List;

import com.wedding.company.dto.CompanyDTO;

public interface CompanyWishService {

    /** 찜 여부 확인 */
    boolean check(String memberEmail, Long cmno);

    /** 찜 등록 (이미 찜한 경우 무시) */
    void add(String memberEmail, Long cmno);

    /** 찜 해제 */
    void remove(String memberEmail, Long cmno);

    /** 마이페이지 찜 업체 목록 조회 (업체 상세 정보 포함) */
    List<CompanyDTO> getMyCompanies(String memberEmail);
}
