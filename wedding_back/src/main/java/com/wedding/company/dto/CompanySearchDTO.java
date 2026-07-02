package com.wedding.company.dto;

import com.wedding.company.domain.CompanyCategory;
import com.wedding.global.dto.PageRequestDTO;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanySearchDTO extends PageRequestDTO {

  private CompanyCategory category;
  private String keyword;
  private BigDecimal minPrice;
  private BigDecimal maxPrice;
  private String sort;
}
