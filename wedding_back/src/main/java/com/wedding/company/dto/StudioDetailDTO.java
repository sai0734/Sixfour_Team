package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudioDetailDTO {

  private Long sdno;

  private String concept;

  private String themeTags;

  private Integer shootingHours;

  private Boolean originalProvided;

  private Boolean retouchIncluded;
}
