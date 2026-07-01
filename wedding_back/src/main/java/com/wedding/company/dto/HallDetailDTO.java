package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HallDetailDTO {

  private Long hdno;

  private String hallType;

  private String representative;

  private Integer capacity;

  private String mealType;

  private Boolean parkingAvailable;

  private String weddingTime;

  private String hallDescription;

  private String imageUrl;
}
