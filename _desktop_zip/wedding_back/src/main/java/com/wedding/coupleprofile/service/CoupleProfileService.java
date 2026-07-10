package com.wedding.coupleprofile.service;

import java.util.List;

import com.wedding.coupleprofile.dto.CoupleProfileDTO;

public interface CoupleProfileService {

    Long register(CoupleProfileDTO coupleProfileDTO);

    CoupleProfileDTO getByMember(String memberEmail);

    void modify(CoupleProfileDTO coupleProfileDTO, String requesterEmail);

    void remove(Long profileId, String requesterEmail);

    // 매칭 화면용 - 본인 프로필 제외 전체 목록
    List<CoupleProfileDTO> listOthers(String excludeMemberEmail);

}
