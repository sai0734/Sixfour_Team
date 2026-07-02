package com.wedding.product.service;

import com.wedding.product.dto.WishDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface WishService {

    List<WishDTO> listByMember(String memberEmail);

    Long register(String memberEmail, Long pno);

    void remove(Long wno);

    void removeByMemberAndProduct(String memberEmail, Long pno);

    boolean isWished(String memberEmail, Long pno);

}
