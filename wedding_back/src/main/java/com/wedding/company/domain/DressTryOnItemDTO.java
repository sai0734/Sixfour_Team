package com.wedding.company.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DressTryOnItemDTO {

  private Long dressItemId;
  private Long cmno;
  private String companyName;
  private String itemName;
  private BigDecimal price;
  private String imageUrl;
  private DressItemType itemType;
  private String sizeRange;
}
