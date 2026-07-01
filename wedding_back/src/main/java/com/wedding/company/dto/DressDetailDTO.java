package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DressDetailDTO {

  private Long ddno;

  private String dressStyle;

  private Integer fittingPrice;

  private Boolean premiumLineAvailable;

  private String rentalPeriod;

  private String sizeRange;
}
