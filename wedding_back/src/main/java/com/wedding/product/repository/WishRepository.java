package com.wedding.product.repository;

import com.wedding.product.domain.Wish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    // 찜 목록 최신순으로 조회
    @EntityGraph(attributePaths = "product")
    @Query("select w from Wish w where w.member.email = :memberEmail order by w.wno desc")
    List<Wish> listWish(@Param("memberEmail") String memberEmail);

    // 중복 찜인지 확인
    @Query("select case when count(w) > 0 then true else false end " +
            "from Wish w where w.member.email = :memberEmail and w.product.pno = :pno")
    boolean existsWish(@Param("memberEmail") String memberEmail, @Param("pno") Long pno);

    // 찜 취소 대상 조회 (실제 삭제는 Service에서 deleteById로 별도 수행)
    @Query("select w from Wish w where w.member.email = :memberEmail and w.product.pno = :pno")
    Optional<Wish> findWish(@Param("memberEmail") String memberEmail, @Param("pno") Long pno);

}