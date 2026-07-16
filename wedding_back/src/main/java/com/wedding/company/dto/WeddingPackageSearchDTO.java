package com.wedding.company.dto;

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
public class WeddingPackageSearchDTO extends PageRequestDTO {

  private String keyword;
  private String sort;
}
