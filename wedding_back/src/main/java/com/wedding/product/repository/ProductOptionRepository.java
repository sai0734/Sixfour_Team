package com.wedding.product.repository;

import com.wedding.product.domain.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    // 상품 옵션 리스트 전부 조회
    @Query("select po from ProductOption po where po.product.pno = :pno order by po.pono asc")
    List<ProductOption> listOption(@Param("pno") Long pno);

    // 상품 옵션 1개 조회
    @Query("select po from ProductOption po where po.pono = :pono and po.product.pno = :pno")
    Optional<ProductOption> findOption(@Param("pono") Long pono, @Param("pno") Long pno);


}
