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
}
