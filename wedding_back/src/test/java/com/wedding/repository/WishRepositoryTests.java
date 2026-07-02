package com.wedding.repository;

import com.wedding.product.domain.Product;
import com.wedding.product.domain.Wish;
import com.wedding.product.repository.WishRepository;
import com.wedding.member.domain.Member;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Log4j2
public class WishRepositoryTests {

    @Autowired
    private WishRepository wishRepository;

    @Test
    public void addWish() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        Member member = Member.builder().email(email).build();
        Product product = Product.builder().pno(pno).build();

        Wish wish = Wish.builder().member(member).product(product).build();

        Wish saved = wishRepository.save(wish);

        log.info("등록된 wno: " + saved.getWno());

    }

    @Test
    public void listWish() {

        String email = "jjj@jjj.com";

        List<Wish> wishes = wishRepository.listWish(email);

        wishes.forEach(wish -> {
            log.info("wno: " + wish.getWno()
                    + "상품명: " + wish.getProduct().getPname()
                    + "가격: " + wish.getProduct().getPrice());
        });

    }

    @Test
    public void existsWish() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        boolean exists = wishRepository.existsWish(email, pno);

        log.info("찜 여부: " + exists);

    }

    @Test
    public void findWish() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        Optional<Wish> result = wishRepository.findWish(email, pno);

        Wish wish = result.orElseThrow();

        log.info("찾은 wno: " + wish.getWno());

    }

    @Test
    public void deleteWish() {

        String email = "jjj@jjj.com";
        Long pno = 31L;

        Optional<Wish> result = wishRepository.findWish(email, pno);

        Wish wish = result.orElseThrow();

        wishRepository.deleteById(wish.getWno());

        log.info("삭제된 wno: " + wish.getWno());

    }

}