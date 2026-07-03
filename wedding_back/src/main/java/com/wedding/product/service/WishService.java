package com.wedding.product.service;

import com.wedding.product.dto.WishDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface WishService {

    // 찜 목록 최신순으로 조회
    List<WishDTO> listByMember(String memberEmail);

    // 찜 등록 (중복 체크)
    Long register(String memberEmail, Long pno);

    // 찜 삭제
    void remove(Long wno);

    // 찜 취소 (하트 토글용)
    void removeByMemberAndProduct(String memberEmail, Long pno);

    // 찜 여부 확인 (하트 채우기용)
    boolean isWished(String memberEmail, Long pno);

}
