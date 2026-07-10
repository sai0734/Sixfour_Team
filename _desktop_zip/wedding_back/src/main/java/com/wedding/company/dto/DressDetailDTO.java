package com.wedding.company.dto;

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
public class DressDetailDTO {

  private Long cmno;
  private String sizeRange;

  @Builder.Default
  private List<DressItemDTO> items = new ArrayList<>();
}
