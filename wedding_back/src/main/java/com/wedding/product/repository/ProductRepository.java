package com.wedding.product.repository;

import com.wedding.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
