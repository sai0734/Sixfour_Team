package com.wedding.companywish.service;

import java.util.List;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.companywish.dto.CompanyWishItemDTO;

public interface CompanyWishService {

    /** 찜 여부 확인 (옵션 상관없이 이 업체를 하나라도 찜했는지) */
    boolean check(String memberEmail, Long cmno);

    /** 찜 등록 - 옵션 없는 업체용 (이미 찜한 경우 무시) */
    void add(String memberEmail, Long cmno);

    /** 찜 해제 - 하트 버튼용. 옵션 상관없이 해당 업체에 대한 찜을 전부 삭제 */
    void remove(String memberEmail, Long cmno);

    /** 마이페이지 찜 업체 목록 조회 (업체 상세 정보 포함) */
    List<CompanyDTO> getMyCompanies(String memberEmail);

    // ↓↓↓ 재원 추가 - 홀/드레스/메이크업처럼 옵션이 있는 업체를 옵션과 함께 찜하는 기능

    /** 옵션과 함께 찜 등록 (이미 같은 옵션으로 찜한 경우 무시) */
    void addWithOption(String memberEmail, Long cmno, String optionName, Integer optionAmount, String optionImage);

    /** wishId로 정확히 찜 해제 (마이페이지 카드/전체선택 삭제용, 본인 소유만) */
    void removeByWishId(String memberEmail, Long wishId);

    /** 마이페이지 찜 업체 목록 조회 - wishId/optionName 포함 (옵션별로 여러 건 표시) */
    List<CompanyWishItemDTO> getMyCompanyWishItems(String memberEmail);

    /** 업체 상세페이지에서 옵션별 하트(찜) 표시용 - 이 업체에서 내가 찜한 옵션명 목록 */
    List<String> getWishedOptionNames(String memberEmail, Long cmno);

    /** 특정 옵션 하나만 찜 해제 (업체 상세페이지 옵션 하트 토글용) */
    void removeOption(String memberEmail, Long cmno, String optionName);
    // ↑↑↑ 재원 추가

}
