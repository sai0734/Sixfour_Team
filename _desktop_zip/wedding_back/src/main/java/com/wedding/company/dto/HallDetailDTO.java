package com.wedding.company.dto;

import com.wedding.company.domain.HallType;
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
public class HallDetailDTO {

  private Long cmno;
  private String hallName;
  private String address;
  private Double latitude;
  private Double longitude;
  private String phone;
  private String representative;
  private HallType hallType;
  private String description;
  private String imageUrl;

  @Builder.Default
  private List<HallItemDTO> items = new ArrayList<>();
}
