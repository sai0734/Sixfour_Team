package com.wedding.company.repository;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  @EntityGraph(attributePaths = "imageList")
  @Query("""
      select c from Company c
      where c.delFlag = false
        and (:category is null or c.category = :category)
        and (:keyword is null or c.name like concat('%', :keyword, '%') or c.address like concat('%', :keyword, '%'))
        and (:minPrice is null or c.priceAvg >= :minPrice)
        and (:maxPrice is null or c.priceAvg <= :maxPrice)
      """)
  Page<Company> searchList(@Param("category") CompanyCategory category,
                           @Param("keyword") String keyword,
                           @Param("minPrice") BigDecimal minPrice,
                           @Param("maxPrice") BigDecimal maxPrice,
                           Pageable pageable);

  @EntityGraph(attributePaths = "imageList")
  @Query("select c from Company c where c.cmno = :cmno")
  Optional<Company> selectOne(@Param("cmno") Long cmno);

  @Modifying
  @Query("update Company c set c.delFlag = :delFlag where c.cmno = :cmno")
  void updateDelFlag(@Param("cmno") Long cmno, @Param("delFlag") boolean delFlag);

  // 회원 이메일로 그 회원이 담당하고 있는 업체 찾기 (담당자는 업체 1곳만 맡음)
  Optional<Company> findByManagerEmail(String managerEmail);

  // 담당자가 지정된 업체 전체 (관리자 회원관리 "담당자 탭"용)
  List<Company> findByManagerEmailIsNotNull();
}