package com.wedding.company.repository;

import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  // 특정 타입(예: SUIT)의 아이템을 하나라도 보유한 업체 번호 목록
  @Query("select distinct di.company.cmno from DressItem di "
      + "where di.itemType = :itemType and di.company.delFlag = false")
  List<Long> findCompanyCmnosByItemType(@Param("itemType") DressItemType itemType);

  // 삭제되지 않은 업체의 아이템 전체 - Pageable의 Sort/개수 제한을 그대로 활용
  @Query("select di from DressItem di where di.company.delFlag = false")
  List<DressItem> findAllActive(Pageable pageable);

  // AI 웨딩플랜 - 드레스 스타일 취향 없이(또는 매칭 실패 후) 아이템 단위로 예산/지역만으로 찾을 때.
  // 5단계(아이템 단위 추천) 이전엔 이 경로도 업체 단위(CompanyRepository)로 찾았는데, 이제 처음부터
  // 구체적인 아이템 하나를 고르도록 바꿈 - 그래야 결과 화면에 뜨는 이미지/가격이 실제로 그 아이템 것이 됨.
  @Query("select di from DressItem di where di.company.delFlag = false "
      + "and (:region is null or di.company.address like concat('%', :region, '%')) "
      + "and (:maxPrice is null or di.price <= :maxPrice)")
  List<DressItem> searchByBudget(
      @Param("region") String region,
      @Param("maxPrice") BigDecimal maxPrice,
      Sort sort);
}
