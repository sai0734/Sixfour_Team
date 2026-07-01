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
@Table(name = "tbl_studio_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class StudioDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long sdno;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_cno", nullable = false)
  private Company company;

  private String concept;

  private String themeTags;

  private Integer shootingHours;

  private Boolean originalProvided;

  private Boolean retouchIncluded;

  public void changeDetail(String concept, String themeTags, Integer shootingHours,
                           Boolean originalProvided, Boolean retouchIncluded) {
    this.concept = concept;
    this.themeTags = themeTags;
    this.shootingHours = shootingHours;
    this.originalProvided = originalProvided;
    this.retouchIncluded = retouchIncluded;
  }
}
