package com.wedding.company.repository;

import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MealType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallItemRepository extends JpaRepository<HallItem, Long> {
  List<HallItem> findByCompany_CmnoOrderByOrdAsc(Long cmno);
  void deleteByCompany_Cmno(Long cmno);

  // 특정 식사 타입(예: BUFFET)의 대관 옵션을 하나라도 보유한 업체 번호 목록
  @Query("select distinct hi.company.cmno from HallItem hi "
      + "where hi.mealType = :mealType and hi.company.delFlag = false")
  List<Long> findCompanyCmnosByMealType(@Param("mealType") MealType mealType);

  // 삭제되지 않은 업체의 대관 옵션 전체 - Pageable의 Sort/개수 제한을 그대로 활용
  @Query("select hi from HallItem hi where hi.company.delFlag = false")
  List<HallItem> findAllActive(Pageable pageable);
}
