package com.wedding.aidress.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiDressTryOnResponseDTO {

  /** data:image/png;base64,... (서버 파일 없음) */
  private String resultImageUrl;

  private String resultImageBase64;
  private Long dressItemId;
  private String dressName;
  private String backgroundPrompt;
}
