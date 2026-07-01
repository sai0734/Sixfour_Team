package com.wedding.weddingplan.service;

import com.wedding.weddingplan.dto.WeddingPlanDTO;

public interface WeddingPlanService {

    Long register(WeddingPlanDTO weddingPlanDTO);

    WeddingPlanDTO get(Long weddingPlanId);

    // 1:1 관계라 회원 이메일로 단건 조회 (마이페이지 허브 진입 시 사용)
    WeddingPlanDTO getByMember(String memberEmail);

    void modify(WeddingPlanDTO weddingPlanDTO);

    void remove(Long weddingPlanId);

}
