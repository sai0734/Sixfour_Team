package com.wedding.aidress.dto;

import lombok.Data;

@Data
public class AiDressTryOnRequestDTO {

  private Long dressItemId;
  private String photoFileName; // 비우면 저장된 내 사진 사용
}
