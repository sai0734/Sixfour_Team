package com.wedding.company.dto;

import com.wedding.company.domain.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyListDTO {

  private Long cno;

  private String name;

  private CompanyType type;

  private String region;

  private String address;

  private Double latitude;

  private Double longitude;

  private Integer price;

  private String description;

  private double ratingAvg;

  private int reviewCount;

  private String mainImage;
}
