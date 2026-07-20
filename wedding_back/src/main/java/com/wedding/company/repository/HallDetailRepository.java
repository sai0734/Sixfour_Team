package com.wedding.company.repository;

import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallDetailRepository extends JpaRepository<HallDetail, Long> {
  Optional<HallDetail> findByCompany_Cmno(Long cmno);

  // AI 웨딩플랜 자세히 모드 - 홀 분위기(hallType) 취향 매칭용. 예산에 가장 근접한(비싼) 순으로 정렬.
  // guestCount는 하객수 필터 - maxCapacity가 null인(아직 데이터 없는) 홀은 "수용 가능"으로 간주해 안 걸러냄.
  @Query("select hd from HallDetail hd where hd.hallType = :hallType and hd.company.delFlag = false "
          + "and (:region is null or hd.company.address like %:region%) "
          + "and (:maxPrice is null or hd.company.priceAvg <= :maxPrice) "
          + "and (:guestCount is null or hd.maxCapacity is null or hd.maxCapacity >= :guestCount) "
          + "order by hd.company.priceAvg desc")
  List<HallDetail> searchByType(@Param("hallType") HallType hallType,
                                @Param("region") String region,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("guestCount") Integer guestCount);

  // AI 웨딩플랜 - 홀 분위기 취향이 없거나 매칭 실패했을 때 예산/지역/하객수만으로 찾는 경로
  // (AiPlanCandidateBuilder의 개별 조합 선택, AiPlanAiServiceImpl의 AI 후보 풀 둘 다에서 씀)
  @Query("select hd from HallDetail hd where hd.company.delFlag = false "
          + "and (:region is null or hd.company.address like %:region%) "
          + "and (:maxPrice is null or hd.company.priceAvg <= :maxPrice) "
          + "and (:guestCount is null or hd.maxCapacity is null or hd.maxCapacity >= :guestCount)")
  List<HallDetail> searchByCapacity(@Param("region") String region,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("guestCount") Integer guestCount,
                                    Sort sort);
}
