package com.wedding.aidress.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiDressTryOnResponseDTO {

  private String resultImageUrl;
  private Long dressItemId;
  private String dressName;
}
