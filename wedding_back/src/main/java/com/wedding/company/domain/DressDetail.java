package com.wedding.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_dress_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class DressDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long ddno;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_cno", nullable = false)
  private Company company;

  private String dressStyle;

  private Integer fittingPrice;

  private Boolean premiumLineAvailable;

  private String rentalPeriod;

  private String sizeRange;

  public void changeDetail(String dressStyle, Integer fittingPrice,
                           Boolean premiumLineAvailable, String rentalPeriod,
                           String sizeRange) {
    this.dressStyle = dressStyle;
    this.fittingPrice = fittingPrice;
    this.premiumLineAvailable = premiumLineAvailable;
    this.rentalPeriod = rentalPeriod;
    this.sizeRange = sizeRange;
  }
}
