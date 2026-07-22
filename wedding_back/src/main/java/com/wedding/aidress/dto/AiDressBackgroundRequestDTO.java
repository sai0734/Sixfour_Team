package com.wedding.aidress.dto;

import lombok.Data;

@Data
public class AiDressBackgroundRequestDTO {
  /** CatVTON 결과 PNG base64 (data: 접두사 있어도 됨) */
  private String imageBase64;
  private String backgroundPrompt;
}
