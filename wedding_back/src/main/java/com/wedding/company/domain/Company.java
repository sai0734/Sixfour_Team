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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_company")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "imageList")
public class Company extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cmno;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private CompanyCategory category;

  @Column(nullable = false)
  private String name;

  private String ceoName;

  private String phone;

  private String address;

  private Double latitude;

  private Double longitude;

  @Column(length = 2000)
  private String description;

  private BigDecimal priceAvg;

  @Column(columnDefinition = "boolean default false")
  private boolean delFlag;

  // 이 업체의 문의 답변을 전담하는 회원(담당자)의 이메일. 관리자가 회원관리에서 임명함.
  // 담당자로 지정된 회원은 로그인 시 문의 답변 전용 페이지로만 이동하도록 프론트에서 강제함
  private String managerEmail;

  public void assignManager(String managerEmail) {
    this.managerEmail = managerEmail;
  }

  public void unassignManager() {
    this.managerEmail = null;
  }

  @ElementCollection
  @CollectionTable(name = "tbl_company_image", joinColumns = @JoinColumn(name = "company_cmno"))
  @Builder.Default
  private List<CompanyImage> imageList = new ArrayList<>();

  public void change(String name, String ceoName, CompanyCategory category, String phone, String address,
                     Double latitude, Double longitude, String description, BigDecimal priceAvg) {
    this.name = name;
    this.ceoName = ceoName;
    this.category = category;
    this.phone = phone;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.description = description;
    this.priceAvg = priceAvg;
  }

  public void changeDelFlag(boolean delFlag) {
    this.delFlag = delFlag;
  }

  public void addImage(String fileName) {
    imageList.add(CompanyImage.builder().fileName(fileName).ord(imageList.size()).build());
  }

  public void clearImages() {
    imageList.clear();
  }
}