package com.wedding.product.repository;

import com.wedding.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>{

  // 상품 전체 리스트 조회하기 (대표이미지 1개, 삭제안된 상품)
  @Query("select p, pi  from Product p left join p.imageList pi  where pi.ord = 0 and p.delFlag = false ")
  Page<Object[]> selectProductList(Pageable pageable);

  // 상품 1개 조회하기
  @EntityGraph(attributePaths = "imageList")
  @Query("select p from Product p where p.pno = :pno")
    Optional<Product> selectProductOne(@Param("pno") Long pno);

  // 상품 소프트 삭제
  @Modifying(clearAutomatically = true)
  @Query("update Product p set p.delFlag = :flag where p.pno = :pno")
    void softDelete(@Param("pno") Long pno,  @Param("flag") boolean flag);

  // 카테고리(다중) + 검색어 + 가격범위 + 평점으로 상품 리스트 조회하기
  @Query("select p, pi from Product p left join p.imageList pi " +
          "where p.delFlag = false " +
          "and (pi.ord = 0 or pi is null) " +
          "and (:categories is null or p.category in :categories) " +
          "and (:keyword is null or :keyword = '' " +
          "     or p.pname like concat('%', :keyword, '%') " +
          "     or p.pdesc like concat('%', :keyword, '%')) " +
          "and (:minPrice is null or p.price >= :minPrice) " +
          "and (:maxPrice is null or p.price <= :maxPrice) " +
          "and (:minRating is null or p.ratingAvg >= :minRating)")
  Page<Object[]> searchProductList(@Param("categories") List<String> categories,
                                   @Param("keyword") String keyword,
                                   @Param("minPrice") Integer minPrice,
                                   @Param("maxPrice") Integer maxPrice,
                                   @Param("minRating") Double minRating,
                                   Pageable pageable);

  // 카테고리 목록 조회하기 (필터용, 중복 제거)
  @Query("select distinct p.category from Product p " +
          "where p.delFlag = false and p.category is not null order by p.category")
  List<String> findDistinctCategories();

  // 관리자용 상풍 리스트 조회 (숨김 상품도 전부 포함)
  @Query("select p, pi from Product p left join p.imageList pi " +
          "where (pi.ord = 0 or pi is null) " +
          "and (:keyword is null or :keyword = '' or p.pname like concat('%', :keyword, '%')) " +
          "and (:category is null or :category = '' or p.category = :category) " +
          "and (:saleStatus is null or :saleStatus = '' " +
          "     or (:saleStatus = 'HIDDEN' and p.delFlag = true) " +
          "     or (:saleStatus = 'SOLD_OUT' and p.delFlag = false and p.stockQty <= 0) " +
          "     or (:saleStatus = 'ON_SALE' and p.delFlag = false and p.stockQty > 0))")
  Page<Object[]> adminSearchProductList(@Param("keyword") String keyword,
                                        @Param("category") String category,
                                        @Param("saleStatus") String saleStatus,
                                        Pageable pageable);

  // 관리자 대시보드용 집계
  long countByDelFlagFalse();

  long countByDelFlagFalseAndStockQtyLessThanEqual(int threshold);

  // 대시보드 "재고 부족 상품" 목록 (상위 5개만)
  List<Product> findTop5ByDelFlagFalseAndStockQtyLessThanEqualOrderByStockQtyAsc(int threshold);

}
