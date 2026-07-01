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
@Table(name = "tbl_makeup_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class MakeupDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long mdno;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_cno", nullable = false)
  private Company company;

  private Boolean hairIncluded;

  private Boolean makeupIncluded;

  private Boolean nailIncluded;

  private Boolean visitAvailable;

  private String makeupStyle;

  private String staffInfo;

  private Integer hairPrice;

  private Integer makeupPrice;

  private Integer nailPrice;

  public void changeDetail(Boolean hairIncluded, Boolean makeupIncluded, Boolean nailIncluded,
                           Boolean visitAvailable, String makeupStyle, String staffInfo,
                           Integer hairPrice, Integer makeupPrice, Integer nailPrice) {
    this.hairIncluded = hairIncluded;
    this.makeupIncluded = makeupIncluded;
    this.nailIncluded = nailIncluded;
    this.visitAvailable = visitAvailable;
    this.makeupStyle = makeupStyle;
    this.staffInfo = staffInfo;
    this.hairPrice = hairPrice;
    this.makeupPrice = makeupPrice;
    this.nailPrice = nailPrice;
  }
}
