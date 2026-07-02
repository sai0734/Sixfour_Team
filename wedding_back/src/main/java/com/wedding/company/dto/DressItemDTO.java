package com.wedding.company.dto;

import com.wedding.company.domain.DressItemType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DressItemDTO {

  private Long dressItemId;
  private Long cmno;
  private String itemName;
  private BigDecimal price;
  private String imageUrl;
  private Integer ord;
  private DressItemType itemType;
  private String styleTags;
  private String sizeRange;
}
