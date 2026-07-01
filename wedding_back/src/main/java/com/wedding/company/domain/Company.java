package com.wedding.company.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_company")
@Getter
@ToString(exclude = "imageList")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Company extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cno;

  @Column(nullable = false)
  private String name;

  private String ceoName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CompanyType type;

  private String region;

  private String address;

  private Double latitude;

  private Double longitude;

  private String phone;

  private Integer price;

  @Column(length = 2000)
  private String description;

  @Column(columnDefinition = "double default 0.0")
  private double ratingAvg;

  @Column(columnDefinition = "int default 0")
  private int reviewCount;

  @Column(columnDefinition = "bigint default 0")
  private Long viewCount;

  @Column(columnDefinition = "boolean default false")
  private boolean delFlag;

  @ElementCollection
  @CollectionTable(name = "tbl_company_image", joinColumns = @JoinColumn(name = "company_cno"))
  @Builder.Default
  private List<CompanyImage> imageList = new ArrayList<>();

  public void changeBasic(String name, String ceoName, CompanyType type, String region, String address,
                          Double latitude, Double longitude, String phone,
                          Integer price, String description) {
    this.name = name;
    this.ceoName = ceoName;
    this.type = type;
    this.region = region;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.phone = phone;
    this.price = price;
    this.description = description;
  }

  public void changeDel(boolean delFlag) {
    this.delFlag = delFlag;
  }

  public void addImage(CompanyImage image) {
    image.setOrd(this.imageList.size());
    imageList.add(image);
  }

  public void addImageString(String fileName) {
    CompanyImage companyImage = CompanyImage.builder()
        .fileName(fileName)
        .build();
    addImage(companyImage);
  }

  public void clearImages() {
    this.imageList.clear();
  }
}
