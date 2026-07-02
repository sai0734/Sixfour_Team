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
}
