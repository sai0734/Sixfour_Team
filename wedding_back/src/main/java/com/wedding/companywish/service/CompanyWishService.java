package com.wedding.companywish.service;

import java.util.List;

import com.wedding.companywish.dto.CompanyWishDTO;

public interface CompanyWishService {

    Long register(CompanyWishDTO companyWishDTO);

    List<CompanyWishDTO> listByMember(String memberEmail);

    void remove(Long wishId);

    // memberEmail + cmno로 찜 취소 (프론트에서 하트 아이콘 토글 시 wishId를 몰라도 되게)
    void removeByMemberAndCompany(String memberEmail, Long cmno);

}
