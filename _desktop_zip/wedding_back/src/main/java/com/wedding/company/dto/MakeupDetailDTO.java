package com.wedding.company.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MakeupDetailDTO {

  private Long cmno;
  private Boolean includesHairService;
  private Boolean includesMakeupService;
  private Boolean includesNailService;
  private BigDecimal hairPrice;
  private BigDecimal makeupPrice;
  private BigDecimal nailPrice;

  @Builder.Default
  private List<MakeupPackageDTO> packages = new ArrayList<>();
}
