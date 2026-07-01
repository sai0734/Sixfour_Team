package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MakeupDetailDTO {

  private Long mdno;

  private Boolean hairIncluded;

  private Boolean makeupIncluded;

  private Boolean nailIncluded;

  private Boolean visitAvailable;

  private String makeupStyle;

  private String staffInfo;

  private Integer hairPrice;

  private Integer makeupPrice;

  private Integer nailPrice;
}
