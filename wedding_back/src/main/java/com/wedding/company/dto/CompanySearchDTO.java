package com.wedding.company.dto;

import com.wedding.company.domain.CompanyType;
import com.wedding.global.dto.PageRequestDTO;
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

  private CompanyType type;

  private String region;

  private String keyword;

  private Integer minPrice;

  private Integer maxPrice;

  private String sort;
}
