package com.wedding.product.repository;

import com.wedding.product.domain.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 상품의 최상위 리뷰 목록 조회 (답변 제외, 최신순)
    @EntityGraph(attributePaths = {"member"})
    @Query("select r from Review r where r.product.pno = :pno and r.review is null order by r.rno desc")
    List<Review> listTopReviewsByProduct(@Param("pno") Long pno);

    // 특정 리뷰에 달린 답변 목록 조회
    @EntityGraph(attributePaths = {"member"})
    @Query("select r from Review r where r.review.rno = :rno order by r.rno asc")
    List<Review> listRepliesByReview(@Param("rno") Long rno);

    // 상품의 평균 평점 (답변 제외, 평점 있는 것만)
    @Query("select avg(r.rating) from Review r where r.product.pno = :pno and r.rating is not null")
    Double getAverageRating(@Param("pno") Long pno);

    // 상품의 리뷰 개수 (답변 제외)
    @Query("select count(r) from Review r where r.product.pno = :pno and r.review is null")
    long countByProduct(@Param("pno") Long pno);

    // 리뷰 1개 조회 (상품 소속 검증 포함)
    @Query("select r from Review r where r.rno = :rno and r.product.pno = :pno")
    Optional<Review> findOneByProduct(@Param("rno") Long rno, @Param("pno") Long pno);
}