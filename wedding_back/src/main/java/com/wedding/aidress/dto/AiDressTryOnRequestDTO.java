package com.wedding.aidress.dto;

import lombok.Data;

@Data
public class AiDressTryOnRequestDTO {

  private Long dressItemId;
  private String photoFileName; // 비우면 저장된 내 사진 사용

  /** 선택: 배경 장면을 설명하는 프롬프트. 비우면 CatVTON 결과만 사용 */
  private String backgroundPrompt;
}
