package com.wedding.company.dto;

import com.wedding.company.domain.CompanyCategory;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyListDTO {

  private Long cmno;
  private CompanyCategory category;
  private String name;
  private String phone;
  private String address;
  private Double latitude;
  private Double longitude;
  private String description;
  private BigDecimal priceAvg;
  private String mainImage;
}
