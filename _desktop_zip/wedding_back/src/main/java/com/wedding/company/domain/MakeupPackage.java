package com.wedding.company.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "tbl_makeup_package")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "company")
public class MakeupPackage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long packageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cmno", nullable = false)
  private Company company;

  @Enumerated(EnumType.STRING)
  private MakeupPackageType packageType;

  private BigDecimal discountRate;
}
