package com.wedding.company.domain;

import jakarta.persistence.Column;
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
@Table(name = "tbl_studio_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class StudioDetail {

  @Id
  private Long cmno;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cmno")
  private Company company;

  @Column(length = 1000)
  private String themeTags;

  public void change(String themeTags) {
    this.themeTags = themeTags;
  }
}
