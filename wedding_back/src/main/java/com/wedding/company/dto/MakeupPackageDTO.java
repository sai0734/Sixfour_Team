package com.wedding.company.dto;

import com.wedding.company.domain.MakeupPackageType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MakeupPackageDTO {

  private Long packageId;
  private Long cmno;
  private MakeupPackageType packageType;
  private BigDecimal discountRate;
}
