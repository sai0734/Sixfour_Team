package com.wedding.aidress.dto;

import lombok.Data;

@Data
public class TryOnHistoryUpdateDTO {

  /** 새 배경 프롬프트. 비우면 CatVTON 원본 결과로 되돌림 */
  private String backgroundPrompt;
}
