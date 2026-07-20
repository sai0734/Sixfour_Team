package com.wedding.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tbl_hall_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class HallDetail {

  @Id
  private Long cmno;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cmno")
  private Company company;

  private String hallName;
  private String address;
  private Double latitude;
  private Double longitude;
  private String phone;
  private String representative;

  @Enumerated(EnumType.STRING)
  private HallType hallType;

  @Column(length = 2000)
  private String description;

  private String imageUrl;

  // 최대 수용 인원 - AI 웨딩플랜 하객수 필터용. 기존 더미데이터엔 값이 없어서 당분간 전부 null이고,
  // null이면 "수용 가능"으로 간주해서 필터에서 제외되지 않는다 (AiPlanCandidateBuilder 쪽 쿼리 참고).
  private Integer maxCapacity;

  public void change(String hallName, String address, Double latitude, Double longitude, String phone,
      String representative, HallType hallType, String description, String imageUrl) {
    this.hallName = hallName;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.phone = phone;
    this.representative = representative;
    this.hallType = hallType;
    this.description = description;
    this.imageUrl = imageUrl;
  }

  public void changeMaxCapacity(Integer maxCapacity) {
    this.maxCapacity = maxCapacity;
  }
}
