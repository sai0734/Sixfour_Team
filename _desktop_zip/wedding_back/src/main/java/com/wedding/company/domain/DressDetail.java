package com.wedding.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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
  private Long cmno;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cmno")
  private Company company;

  private String sizeRange;

  public void change(String sizeRange) {
    this.sizeRange = sizeRange;
  }
}
