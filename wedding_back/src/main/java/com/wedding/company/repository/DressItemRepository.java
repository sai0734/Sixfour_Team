package com.wedding.company.repository;

import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DressItemRepository extends JpaRepository<DressItem, Long> {
  List<DressItem> findByCompany_CmnoOrderByOrdAsc(Long cmno);

  @Modifying
  @Query("DELETE FROM DressItem d WHERE d.company.cmno = :cmno")
  void deleteByCompany_Cmno(@Param("cmno") Long cmno);

  // AI 드레스 체험 — 드레스 상품 목록 (예복 SUIT 제외)
  @Query("""
      select d from DressItem d
      join fetch d.company c
      where c.delFlag = false
        and d.itemType <> :suitType
      order by d.dressItemId desc
      """)
  Page<DressItem> findAllForTryOn(
      @Param("suitType") DressItemType suitType,
      Pageable pageable);

  // AI 웨딩플랜 자세히 모드 - 드레스 스타일(styleTags) 취향 매칭용. 업체가 아니라 상품 단위 태그라
  // 같은 업체 아이템이 여러 개 나올 수 있음 - 서비스단에서 업체 기준으로 중복 제거/정렬함.
  @Query("select di from DressItem di where di.styleTags like concat('%', :keyword, '%') "
      + "and di.company.delFlag = false "
      + "and (:region is null or di.company.address like concat('%', :region, '%')) "
      + "and (:maxPrice is null or di.company.priceAvg <= :maxPrice)")
  List<DressItem> searchByStyleKeyword(
      @Param("keyword") String keyword,
      @Param("region") String region,
      @Param("maxPrice") BigDecimal maxPrice);
}
