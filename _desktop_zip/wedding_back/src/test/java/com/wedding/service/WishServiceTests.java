package com.wedding.service;

import com.wedding.product.dto.WishDTO;
import com.wedding.product.service.WishService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Log4j2
public class WishServiceTests {

    @Autowired
    private WishService wishService;

    @Test
    public void testRegister() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        Long wno = wishService.register(email, pno);

        log.info("등록된 wno: " + wno);

    }

    @Test
    public void testListByMember() {

        String email = "jjj@jjj.com";

        List<WishDTO> list = wishService.listByMember(email);

        list.forEach(dto -> {
            log.info("wno: " + dto.getWno()
                    + "pno: " + dto.getPno()
                    + "상품명: " + dto.getPname()
                    + "가격: " + dto.getPrice()
                    + "썸네일: " + dto.getThumbnail());
        });

    }

    @Test
    public void testIsWished() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        boolean wished = wishService.isWished(email, pno);

        log.info("찜 여부: " + wished);

    }

    @Test
    public void testRemoveByMemberAndProduct() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        wishService.removeByMemberAndProduct(email, pno);

        log.info("찜 취소 완료: " + email + ", pno=" + pno);

    }

    @Test
    public void testRemove() {

        // listByMember() 실행해서 실제 존재하는 wno를 먼저 확인한 후 넣어야 함
        Long wno = 31L;

        wishService.remove(wno);

        log.info("삭제된 wno: " + wno);

    }

}