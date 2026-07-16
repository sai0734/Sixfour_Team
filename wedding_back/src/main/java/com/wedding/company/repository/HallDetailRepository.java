package com.wedding.company.repository;

import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallDetailRepository extends JpaRepository<HallDetail, Long> {
  Optional<HallDetail> findByCompany_Cmno(Long cmno);

  // AI 웨딩플랜 자세히 모드 - 홀 분위기(hallType) 취향 매칭용. 예산에 가장 근접한(비싼) 순으로 정렬.
  @Query("select hd from HallDetail hd where hd.hallType = :hallType and hd.company.delFlag = false "
          + "and (:region is null or hd.company.address like %:region%) "
          + "and (:maxPrice is null or hd.company.priceAvg <= :maxPrice) "
          + "order by hd.company.priceAvg desc")
  List<HallDetail> searchByType(@Param("hallType") HallType hallType,
                                @Param("region") String region,
                                @Param("maxPrice") BigDecimal maxPrice);
}
