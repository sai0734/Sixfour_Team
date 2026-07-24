package com.wedding.product.service;

import com.wedding.member.domain.Member;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.Wish;
import com.wedding.product.dto.WishDTO;
import com.wedding.product.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class WishServiceImpl implements WishService{

    private final WishRepository wishRepository;

    // 찜 목록 최신순으로 조회
    @Override
    public List<WishDTO> listByMember(String memberEmail) {

        List<Wish> wishes = wishRepository.listWish(memberEmail);

        return wishes.stream().map(wish -> {

            Product product = wish.getProduct();

            String thumbnail = product.getImageList().stream()
                    .filter(img -> img.getOrd() == 0)
                    .map(img -> img.getFileName())
                    .findFirst()
                    .orElse(null);

            return WishDTO.builder()
                    .wno(wish.getWno())
                    .memberEmail(memberEmail)
                    .pno(product.getPno())
                    .pname(product.getPname())
                    .price(product.getPrice())
                    .thumbnail(thumbnail)
                    .build();

        }).collect(Collectors.toList());
    }

    // 찜 등록 (중복 체크)
    @Override
    public Long register(String memberEmail, Long pno) {

        boolean exists = wishRepository.existsWish(memberEmail, pno);

        if(exists) {
            throw new IllegalStateException("이미 찜한 상품입니다. memberEmail=" + memberEmail + ", pno=" + pno);
        }

        Member member = Member.builder().email(memberEmail).build();
        Product product = Product.builder().pno(pno).build();

        Wish wish = Wish.builder().member(member).product(product).build();

        Wish saved = wishRepository.save(wish);

        return saved.getWno();

    }

    // 찜 취소 (하트 토글용)
    @Override
    public void removeByMemberAndProduct(String memberEmail, Long pno) {

        Optional<Wish> result = wishRepository.findWish(memberEmail, pno);

        Wish wish = result.orElseThrow(() -> new NoSuchElementException("찜한 기록이 없습니다. memberEmail=" + memberEmail + ", pno=" + pno));

        wishRepository.deleteById(wish.getWno());

    }

    // 찜 여부 확인 (하트 채우기용)
    @Override
    public boolean isWished(String memberEmail, Long pno) {

        return wishRepository.existsWish(memberEmail, pno);

    }
}
