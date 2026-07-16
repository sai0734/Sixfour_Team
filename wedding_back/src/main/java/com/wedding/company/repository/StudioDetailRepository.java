package com.wedding.company.repository;

import com.wedding.company.domain.StudioDetail;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudioDetailRepository extends JpaRepository<StudioDetail, Long> {
  Optional<StudioDetail> findByCompany_Cmno(Long cmno);

  // AI 웨딩플랜 자세히 모드 - 스튜디오 분위기(themeTags) 취향 매칭용
  @Query("select sd from StudioDetail sd where sd.themeTags like %:keyword% and sd.company.delFlag = false "
          + "and (:region is null or sd.company.address like %:region%) "
          + "and (:maxPrice is null or sd.company.priceAvg <= :maxPrice) "
          + "order by sd.company.priceAvg desc")
  List<StudioDetail> searchByThemeKeyword(@Param("keyword") String keyword,
                                          @Param("region") String region,
                                          @Param("maxPrice") BigDecimal maxPrice);
}
