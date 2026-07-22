package com.wedding.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_makeup_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class MakeupDetail {

  @Id
  private Long cmno;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cmno")
  private Company company;

  private Boolean includesHairService;
  private Boolean includesMakeupService;
  private Boolean includesNailService;
  private BigDecimal hairPrice;
  private BigDecimal makeupPrice;
  private BigDecimal nailPrice;

  public void change(Boolean includesHairService, Boolean includesMakeupService, Boolean includesNailService,
      BigDecimal hairPrice, BigDecimal makeupPrice, BigDecimal nailPrice) {
    this.includesHairService = includesHairService;
    this.includesMakeupService = includesMakeupService;
    this.includesNailService = includesNailService;
    this.hairPrice = hairPrice;
    this.makeupPrice = makeupPrice;
    this.nailPrice = nailPrice;
  }

  // 이 업체가 그 패키지 타입을 실제로 팔 수 있는지 - 단품/조합은 별도 할인 패키지(MakeupPackage)
  // row가 없어도 취급 서비스(includesXxxService)만 있으면 항상 판매 가능하다(예약 화면
  // companyOptionBuilder.js와 동일 기준). MakeupPackage 테이블은 할인율 조회 전용이라 "판매
  // 여부" 판단엔 쓰면 안 된다 - FULL/HAIR_MAKEUP처럼 할인 행이 있는 조합만 매칭되고 나머지
  // 단품·조합은 전부 못 찾던 버그의 원인이 이 구분을 안 했던 것이었다.
  public boolean supports(MakeupPackageType type) {
    boolean hair = Boolean.TRUE.equals(includesHairService);
    boolean makeup = Boolean.TRUE.equals(includesMakeupService);
    boolean nail = Boolean.TRUE.equals(includesNailService);
    return switch (type) {
      case HAIR -> hair;
      case MAKEUP -> makeup;
      case NAIL -> nail;
      case HAIR_MAKEUP -> hair && makeup;
      case HAIR_NAIL -> hair && nail;
      case MAKEUP_NAIL -> makeup && nail;
      case FULL -> hair && makeup && nail;
    };
  }

  // 취향이 없거나(랜덤 추천) 원하는 패키지를 이 업체가 못 파는 경우, 이 업체가 실제로 팔 수 있는
  // 것 중 가장 풍성한 조합을 대신 보여주기 위한 기본값. 서비스를 하나도 안 취급하면 null.
  public MakeupPackageType bestSupportedType() {
    boolean hair = Boolean.TRUE.equals(includesHairService);
    boolean makeup = Boolean.TRUE.equals(includesMakeupService);
    boolean nail = Boolean.TRUE.equals(includesNailService);
    if (hair && makeup && nail) return MakeupPackageType.FULL;
    if (hair && makeup) return MakeupPackageType.HAIR_MAKEUP;
    if (hair && nail) return MakeupPackageType.HAIR_NAIL;
    if (makeup && nail) return MakeupPackageType.MAKEUP_NAIL;
    if (hair) return MakeupPackageType.HAIR;
    if (makeup) return MakeupPackageType.MAKEUP;
    if (nail) return MakeupPackageType.NAIL;
    return null;
  }
}
