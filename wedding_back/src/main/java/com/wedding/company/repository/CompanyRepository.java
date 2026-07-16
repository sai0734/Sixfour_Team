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

  // AI 웨딩플랜 자세히 모드 - 메이크업은 구조화된 스타일 필드가 없어서(내추럴/청순/화사/시크 같은 값이
  // 데이터에 아예 없음) description 자유텍스트 키워드 매칭으로 대체함. 홀/스튜디오/드레스보다 정확도는 떨어짐.
  @Query("select c from Company c where c.delFlag = false and c.category = :category "
          + "and c.description like %:keyword% "
          + "and (:region is null or c.address like %:region%) "
          + "and (:maxPrice is null or c.priceAvg <= :maxPrice) "
          + "order by c.priceAvg desc")
  List<Company> searchByCategoryDescriptionKeyword(@Param("category") CompanyCategory category,
                                                   @Param("keyword") String keyword,
                                                   @Param("region") String region,
                                                   @Param("maxPrice") BigDecimal maxPrice);

  // 담당자가 지정된 업체 전체 (관리자 회원관리 "담당자 탭"용)
  List<Company> findByManagerEmailIsNotNull();
}