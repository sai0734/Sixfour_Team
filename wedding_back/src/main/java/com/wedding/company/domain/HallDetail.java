package com.wedding.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
@Table(name = "tbl_hall_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class HallDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long hdno;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_cno", nullable = false)
  private Company company;

  private String hallType;

  private String representative;

  private Integer capacity;

  private String mealType;

  private Boolean parkingAvailable;

  private String weddingTime;

  @Column(length = 1000)
  private String hallDescription;

  private String imageUrl;

  public void changeDetail(String hallType, String representative, Integer capacity, String mealType,
                           Boolean parkingAvailable, String weddingTime,
                           String hallDescription, String imageUrl) {
    this.hallType = hallType;
    this.representative = representative;
    this.capacity = capacity;
    this.mealType = mealType;
    this.parkingAvailable = parkingAvailable;
    this.weddingTime = weddingTime;
    this.hallDescription = hallDescription;
    this.imageUrl = imageUrl;
  }
}
