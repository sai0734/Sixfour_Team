package com.wedding.company.dto;

import com.wedding.company.domain.MealType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HallItemDTO {

  private Long hallItemId;
  private Long cmno;
  private String itemName;
  private BigDecimal price;
  private Integer capacity;
  private String imageUrl;
  private Integer ord;
  private MealType mealType;
}
